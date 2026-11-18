import ActivityKit
import SwiftUI
import WidgetKit

/// The home-screen widgets, as Android's four: Now playing 2×2 and 4×2 (one widget, small and medium, as Android's
/// two providers are one widget at two sizes), 4×1, and Now playing + Liked 4×2.
@main
struct SoraWidgetBundle: WidgetBundle {
    var body: some Widget {
        NowPlayingWidget()
        PillWidget()
        LibraryWidget()
        if #available(iOS 18.0, *) {
            RecognizeControl()
        }
        RecognitionLiveActivity()
    }
}

/// Control Center's "Recognize Music" button: opens Sora listening (see SoraRecognizeIntent).
@available(iOS 18.0, *)
struct RecognizeControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "com.zords.soramusic.recognize") {
            ControlWidgetButton(action: SoraRecognizeIntent()) {
                Label("Recognize Music", image: "sora_logo")
            }
        }
        .displayName("Recognize Music")
        .description("Identify the song playing around you with Sora.")
    }
}

/// Music recognition while Sora listens in the background: the mark, "Listening…" / "Identifying…", and Cancel.
/// Tapping it goes back to Sora's Recognize page. Started by the app; see SoraRecognitionAttributes.
struct RecognitionLiveActivity: Widget {
    private static let open = URL(string: "sora://widget/recognize_open")

    var body: some WidgetConfiguration {
        ActivityConfiguration(for: SoraRecognitionAttributes.self) { context in
            HStack(spacing: 14) {
                Image("sora_logo")
                    .font(.title)
                    .foregroundStyle(.white)
                VStack(alignment: .leading, spacing: 2) {
                    Text(context.state.title).font(.headline).foregroundStyle(.white)
                    Text(context.state.subtitle).font(.subheadline).foregroundStyle(.white.opacity(0.7))
                }
                Spacer()
                Image(systemName: "waveform")
                    .font(.title3)
                    .foregroundStyle(.white)
                    .symbolEffect(.variableColor.iterative)
                Button(intent: SoraCancelRecognitionIntent()) {
                    Image(systemName: "xmark")
                }
                .buttonStyle(.bordered)
                .buttonBorderShape(.circle)
                .tint(.white)
            }
            .padding(16)
            .activityBackgroundTint(.black.opacity(0.75))
            .activitySystemActionForegroundColor(.white)
            .widgetURL(Self.open)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Image("sora_logo").font(.title2).padding(.leading, 6)
                }
                DynamicIslandExpandedRegion(.center) {
                    VStack(spacing: 2) {
                        Text(context.state.title).font(.headline)
                        Text(context.state.subtitle).font(.subheadline).foregroundStyle(.secondary)
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Button(intent: SoraCancelRecognitionIntent()) {
                        Image(systemName: "xmark")
                    }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.circle)
                }
            } compactLeading: {
                Image("sora_logo")
            } compactTrailing: {
                Image(systemName: "waveform").symbolEffect(.variableColor.iterative)
            } minimal: {
                Image("sora_logo")
            }
            .widgetURL(Self.open)
        }
    }
}

struct SoraEntry: TimelineEntry {
    let date = Date()
    let snapshot: SoraWidgetSnapshot
    let size: CGSize
    /// The finished picture for the widget's face: the banded cover, or the blurred body.
    let face: UIImage?
    let art: UIImage?
    let tiles: [UIImage?]
}

enum Face { case media(band: CGFloat), body }

/// Reads the snapshot the app wrote and draws this widget's pictures at its size. One entry, never refreshed on a
/// schedule: the app reloads the timelines whenever what they show changes (SoraWidgetStore.save).
struct SoraProvider: TimelineProvider {
    let face: (WidgetFamily) -> Face
    var wantsTiles = false

    func placeholder(in context: Context) -> SoraEntry {
        entry(SoraWidgetSnapshot(mediaId: "", title: "Song title", artist: "Artist", hasPrevious: true, hasNext: true), context, images: false)
    }

    func getSnapshot(in context: Context, completion: @escaping (SoraEntry) -> Void) {
        let s = SoraWidgetStore.load()
        completion(s.hasTrack || !context.isPreview ? entry(s, context, images: true) : placeholder(in: context))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SoraEntry>) -> Void) {
        completion(Timeline(entries: [entry(SoraWidgetStore.load(), context, images: true)], policy: .never))
    }

    private func entry(_ s: SoraWidgetSnapshot, _ context: Context, images: Bool) -> SoraEntry {
        let size = context.displaySize
        let art = images ? image(s.art, maxPx: 720) : nil
        // 2×, not the screen's 3×: WidgetKit silently drops an image it finds too large, and the extension has
        // ~30 MB for the blur's intermediates. MediaWidget's MAX_BITMAP_PX bound on top; at phone sizes it never binds.
        let scale = min(2, 1200 / max(size.width, size.height, 1))
        let faceImage: UIImage?
        switch face(context.family) {
        case .media(let band): faceImage = WidgetArt.media(cover: art, size: size, band: band, scale: scale)
        case .body: faceImage = WidgetArt.body(cover: art, size: size, scale: scale)
        }
        let tiles = wantsTiles && images ? s.liked.map { image($0.art, maxPx: 240) } : []
        return SoraEntry(snapshot: s, size: size, face: faceImage, art: art, tiles: tiles)
    }

    /// A picture the app saved, decoded no larger than [maxPx] (the extension has little memory to spare).
    private func image(_ name: String?, maxPx: CGFloat) -> UIImage? {
        guard let url = SoraWidgetStore.fileURL(name), let img = UIImage(contentsOfFile: url.path) else { return nil }
        let longest = max(img.size.width * img.scale, img.size.height * img.scale)
        guard longest > maxPx else { return img }
        let k = maxPx / longest
        let target = CGSize(width: img.size.width * img.scale * k, height: img.size.height * img.scale * k)
        let format = UIGraphicsImageRendererFormat(); format.scale = 1
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in img.draw(in: CGRect(origin: .zero, size: target)) }
    }
}

// MARK: - Widgets

struct NowPlayingWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "SoraNowPlaying", provider: SoraProvider(face: { family in
            .media(band: family == .systemSmall ? 66 : 60) // widget_band_compact / widget_band_wide
        })) { e in
            Group {
                if e.size.width >= 215 { WideMediaView(s: e.snapshot, art: e.face) } // WIDE_LAYOUT_MIN_DP
                else { CompactMediaView(s: e.snapshot, art: e.face) }
            }
            .soraWidgetBackground(Color.black)
            .widgetURL(openPlayerURL)
        }
        .configurationDisplayName("Now playing")
        .description("Album art with the track and playback controls.")
        .supportedFamilies([.systemSmall, .systemMedium])
        .soraFullBleed()
    }
}

struct PillWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "SoraNowPlayingPill", provider: SoraProvider(face: { _ in .body })) { e in
            PillView(s: e.snapshot, disc: e.art, scale: PillView.scale(for: e.size))
                .soraWidgetBackground(faceView(e.face))
                .widgetURL(openPlayerURL)
        }
        .configurationDisplayName("Now playing 4×1")
        .description("The track with Like, Shuffle and playback controls, in a single row.")
        .supportedFamilies([.systemMedium])
        .soraFullBleed()
    }
}

struct LibraryWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "SoraNowPlayingLibrary", provider: SoraProvider(face: { _ in .body }, wantsTiles: true)) { e in
            LibraryView(s: e.snapshot, art: e.art, tiles: e.tiles, size: e.size)
                .soraWidgetBackground(faceView(e.face))
                .widgetURL(openPlayerURL)
        }
        .configurationDisplayName("Now playing + Liked")
        .description("The track and playback controls, with your liked songs underneath.")
        .supportedFamilies([.systemMedium])
        .soraFullBleed()
    }
}

@ViewBuilder private func faceView(_ face: UIImage?) -> some View {
    if let face { Image(uiImage: face).resizable() } else { placeholderGradient }
}

extension View {
    /// Declared as the widget's background: what the system dims and tints on the Home Screen.
    func soraWidgetBackground<B: View>(_ background: B) -> some View {
        containerBackground(for: .widget) { background }
    }
}

extension WidgetConfiguration {
    /// The pictures run to the tile's edges, as Android's do, rather than inside the system's margins.
    func soraFullBleed() -> some WidgetConfiguration { contentMarginsDisabled() }
}
