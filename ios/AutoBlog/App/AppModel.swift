import Foundation
import SwiftUI

enum MainRoute: Hashable {
    case detail(String)
    case create
    case edit(String)
    case share([String])
}

final class AppModel: ObservableObject {
    @Published var isLoggedIn = false
    @Published var messages: [Message] = Message.sample
    @Published var path = NavigationPath()

    func login(email: String, password: String) -> Bool {
        #if DEBUG
        isLoggedIn = true
        return true
        #else
        if email == "admin@gmail.com" && password == "1234" {
            isLoggedIn = true
            return true
        }
        return false
        #endif
    }

    func sendMessage(title: String, body: String, imageURL: String?) {
        let m = Message(
            id: UUID().uuidString,
            title: title,
            body: body,
            createdAt: Date(),
            imageURLString: imageURL
        )
        messages.insert(m, at: 0)
    }

    func sendUpdate(id: String, title: String, body: String, imageURL: String?) {
        guard let i = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[i].title = title
        messages[i].body = body
        messages[i].imageURLString = imageURL
    }

    func deleteMessage(id: String) {
        messages.removeAll { $0.id == id }
    }

    func deleteMessages(ids: Set<String>) {
        messages.removeAll { ids.contains($0.id) }
    }

    func setImage(id: String, urlString: String?) {
        guard let i = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[i].imageURLString = urlString
    }

    func message(id: String) -> Message? {
        messages.first { $0.id == id }
    }

    func messages(ids: [String]) -> [Message] {
        ids.compactMap { id in messages.first { $0.id == id } }
    }
}
