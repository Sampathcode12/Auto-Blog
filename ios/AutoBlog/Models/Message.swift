import Foundation

struct Message: Identifiable, Hashable {
    var id: String
    var title: String
    var body: String
    var createdAt: Date
    var imageURLString: String?

    var listPreview: String {
        let text = body.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return "" }
        let lines = text.components(separatedBy: .newlines).prefix(2)
        let compact = lines.joined(separator: " ")
        let maxLen = 200
        if compact.count > maxLen {
            return String(compact.prefix(maxLen)).trimmingCharacters(in: .whitespaces) + "…"
        }
        return compact
    }

    var shortDate: String {
        let f = DateFormatter()
        f.dateFormat = "MMM d, yyyy"
        f.locale = Locale(identifier: "en_US")
        return f.string(from: createdAt)
    }

    var detailDate: String {
        let f = DateFormatter()
        f.dateFormat = "EEEE, MMMM d, yyyy 'at' hh:mm a"
        f.locale = Locale(identifier: "en_US")
        return f.string(from: createdAt)
    }

    static let sample: [Message] = {
        var cal = Calendar.current
        cal.timeZone = TimeZone.current
        func date(_ y: Int, _ m: Int, _ d: Int, _ h: Int, _ min: Int) -> Date {
            var c = DateComponents()
            c.year = y; c.month = m; c.day = d; c.hour = h; c.minute = min
            return cal.date(from: c) ?? Date()
        }
        return [
            Message(
                id: "1",
                title: "Travel Memories from Tokyo",
                body: "Just returned from an amazing trip to Tokyo! The blend of traditional temples and modern technology is fascinating. Can't wait to share more photos and stories from this incredible city.",
                createdAt: date(2026, 4, 5, 9, 47),
                imageURLString: nil
            ),
            Message(
                id: "2",
                title: "Tips for Great Writing",
                body: "Start with a clear outline, read your draft out loud, and cut anything that does not move the story forward.",
                createdAt: date(2026, 4, 3, 12, 0),
                imageURLString: nil
            ),
            Message(
                id: "3",
                title: "My First Blog Post",
                body: "Excited to share thoughts on tech, travel, and everyday lessons. Thanks for reading!",
                createdAt: date(2026, 3, 28, 10, 30),
                imageURLString: nil
            )
        ]
    }()

    static func bulkShareText(_ items: [Message]) -> String {
        items.map { "\($0.title)\n\n\($0.body)" }.joined(separator: "\n\n────────\n\n")
    }
}
