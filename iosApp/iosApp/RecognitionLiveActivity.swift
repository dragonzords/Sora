import ActivityKit
import Foundation
import SoraKit

/// Kotlin's RecognitionLiveActivityHost: ActivityKit is Swift-only, so the recognizer's state (RecognitionNotifier)
/// reaches the lock screen and the Dynamic Island through here.
final class RecognitionLiveActivityImpl: NSObject, RecognitionLiveActivityHost {
    /// An Activity<SoraRecognitionAttributes>, typed loosely so this class needs no availability gate itself.
    private var current: Any?

    func show(phase: String, title: String, subtitle: String) {
        guard #available(iOS 16.2, *) else { return }
        let content = ActivityContent(
            state: SoraRecognitionAttributes.ContentState(phase: phase, title: title, subtitle: subtitle),
            staleDate: nil
        )
        if let activity = (current as? ActivityKit.Activity<SoraRecognitionAttributes>)
            ?? ActivityKit.Activity<SoraRecognitionAttributes>.activities.first {
            current = activity
            Task { await activity.update(content) }
        } else if ActivityAuthorizationInfo().areActivitiesEnabled {
            // Fails from the background, which is fine: it is asked for as listening starts, with Sora in front.
            current = try? ActivityKit.Activity.request(attributes: SoraRecognitionAttributes(), content: content)
        }
    }

    /// Every one of them, not just [current]: one started by an earlier launch, or a second request, would
    /// otherwise sit on the lock screen saying "Listening…" for good.
    func end() {
        guard #available(iOS 16.2, *) else { return }
        current = nil
        Task {
            for activity in ActivityKit.Activity<SoraRecognitionAttributes>.activities {
                await activity.end(nil, dismissalPolicy: .immediate)
            }
        }
    }
}
