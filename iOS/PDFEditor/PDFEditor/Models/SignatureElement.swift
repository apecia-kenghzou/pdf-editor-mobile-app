import UIKit
import PDFKit

class SignatureElement {
    var image: UIImage
    var position: CGPoint
    var size: CGSize
    var bounds: CGRect

    init(image: UIImage, position: CGPoint, size: CGSize) {
        self.image = image
        self.position = position
        self.size = size
        self.bounds = CGRect(origin: position, size: size)
    }

    func updateBounds() {
        self.bounds = CGRect(origin: position, size: size)
    }

    func contains(point: CGPoint) -> Bool {
        return bounds.contains(point)
    }

    func move(by delta: CGPoint) {
        position.x += delta.x
        position.y += delta.y
        updateBounds()
    }

    func resize(scale: CGFloat) {
        size.width *= scale
        size.height *= scale
        updateBounds()
    }
}
