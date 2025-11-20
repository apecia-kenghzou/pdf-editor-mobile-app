import UIKit
import PDFKit

class TextElement {
    var text: String
    var position: CGPoint
    var fontSize: CGFloat
    var fontName: String
    var bounds: CGRect

    init(text: String, position: CGPoint, fontSize: CGFloat, fontName: String) {
        self.text = text
        self.position = position
        self.fontSize = fontSize
        self.fontName = fontName

        // Calculate bounds
        let font = UIFont(name: fontName, size: fontSize) ?? UIFont.systemFont(ofSize: fontSize)
        let size = text.size(withAttributes: [.font: font])
        self.bounds = CGRect(origin: position, size: size)
    }

    func updateBounds() {
        let font = UIFont(name: fontName, size: fontSize) ?? UIFont.systemFont(ofSize: fontSize)
        let size = text.size(withAttributes: [.font: font])
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

    func createPDFAnnotation(for page: PDFPage) -> PDFAnnotation {
        let annotation = PDFAnnotation(bounds: bounds, forType: .freeText, withProperties: nil)
        annotation.contents = text
        annotation.font = UIFont(name: fontName, size: fontSize) ?? UIFont.systemFont(ofSize: fontSize)
        annotation.fontColor = .black
        annotation.color = .clear
        return annotation
    }
}
