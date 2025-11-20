import UIKit

class SignatureManager {
    static let shared = SignatureManager()

    private let signatureDirectory: URL

    private init() {
        let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        signatureDirectory = documentsPath.appendingPathComponent("Signatures")

        // Create directory if it doesn't exist
        if !FileManager.default.fileExists(atPath: signatureDirectory.path) {
            try? FileManager.default.createDirectory(at: signatureDirectory, withIntermediateDirectories: true)
        }
    }

    func saveSignature(_ image: UIImage) {
        let filename = "signature_\(Date().timeIntervalSince1970).png"
        let fileURL = signatureDirectory.appendingPathComponent(filename)

        if let data = image.pngData() {
            try? data.write(to: fileURL)
        }
    }

    func getSavedSignatures() -> [UIImage] {
        var signatures: [UIImage] = []

        if let files = try? FileManager.default.contentsOfDirectory(at: signatureDirectory,
                                                                     includingPropertiesForKeys: nil) {
            for fileURL in files {
                if fileURL.pathExtension == "png",
                   let data = try? Data(contentsOf: fileURL),
                   let image = UIImage(data: data) {
                    signatures.append(image)
                }
            }
        }

        return signatures
    }

    func deleteSignature(at index: Int) {
        if let files = try? FileManager.default.contentsOfDirectory(at: signatureDirectory,
                                                                     includingPropertiesForKeys: nil),
           index >= 0 && index < files.count {
            try? FileManager.default.removeItem(at: files[index])
        }
    }
}
