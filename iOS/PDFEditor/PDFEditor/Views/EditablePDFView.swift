import UIKit
import PDFKit

class EditablePDFView: PDFView {

    var textElements: [TextElement] = []
    var signatureElements: [SignatureElement] = []

    private var selectedTextElement: TextElement?
    private var selectedSignatureElement: SignatureElement?
    private var lastTouchPoint: CGPoint?

    weak var editDelegate: EditablePDFViewDelegate?

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setup()
    }

    private func setup() {
        displayMode = .singlePage
        autoScales = true
        displayDirection = .vertical
    }

    override func draw(_ rect: CGRect) {
        super.draw(rect)

        guard let context = UIGraphicsGetCurrentContext() else { return }

        // Draw text elements
        for element in textElements {
            let font = UIFont(name: element.fontName, size: element.fontSize) ?? UIFont.systemFont(ofSize: element.fontSize)
            let attributes: [NSAttributedString.Key: Any] = [
                .font: font,
                .foregroundColor: UIColor.black
            ]

            element.text.draw(at: element.position, withAttributes: attributes)

            // Draw selection border
            if element === selectedTextElement {
                context.setStrokeColor(UIColor.blue.cgColor)
                context.setLineWidth(2)
                context.stroke(element.bounds)
            }
        }

        // Draw signature elements
        for element in signatureElements {
            element.image.draw(in: element.bounds)

            // Draw selection border
            if element === selectedSignatureElement {
                context.setStrokeColor(UIColor.blue.cgColor)
                context.setLineWidth(2)
                context.stroke(element.bounds)
            }
        }
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else {
            super.touchesBegan(touches, with: event)
            return
        }

        let location = touch.location(in: self)
        lastTouchPoint = location

        // Check if touching a text element
        selectedTextElement = nil
        for element in textElements.reversed() {
            if element.contains(point: location) {
                selectedTextElement = element
                setNeedsDisplay()
                return
            }
        }

        // Check if touching a signature element
        selectedSignatureElement = nil
        for element in signatureElements.reversed() {
            if element.contains(point: location) {
                selectedSignatureElement = element
                setNeedsDisplay()
                return
            }
        }

        super.touchesBegan(touches, with: event)
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first, let lastPoint = lastTouchPoint else {
            super.touchesMoved(touches, with: event)
            return
        }

        let location = touch.location(in: self)
        let delta = CGPoint(x: location.x - lastPoint.x, y: location.y - lastPoint.y)

        if let element = selectedTextElement {
            element.move(by: delta)
            lastTouchPoint = location
            setNeedsDisplay()
        } else if let element = selectedSignatureElement {
            element.move(by: delta)
            lastTouchPoint = location
            setNeedsDisplay()
        } else {
            super.touchesMoved(touches, with: event)
        }
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        // Double tap to edit
        guard let touch = touches.first else {
            super.touchesEnded(touches, with: event)
            return
        }

        if touch.tapCount == 2 {
            if let element = selectedTextElement {
                editDelegate?.editablePDFView(self, didDoubleTapTextElement: element)
            }
        }

        super.touchesEnded(touches, with: event)
    }

    func addTextElement(_ element: TextElement) {
        textElements.append(element)
        setNeedsDisplay()
    }

    func addSignatureElement(_ element: SignatureElement) {
        signatureElements.append(element)
        setNeedsDisplay()
    }

    func removeSelectedElement() {
        if let element = selectedTextElement,
           let index = textElements.firstIndex(where: { $0 === element }) {
            textElements.remove(at: index)
            selectedTextElement = nil
            setNeedsDisplay()
        } else if let element = selectedSignatureElement,
                  let index = signatureElements.firstIndex(where: { $0 === element }) {
            signatureElements.remove(at: index)
            selectedSignatureElement = nil
            setNeedsDisplay()
        }
    }

    func getSelectedTextElement() -> TextElement? {
        return selectedTextElement
    }

    func updateTextElement(_ element: TextElement, text: String, fontSize: CGFloat, fontName: String) {
        element.text = text
        element.fontSize = fontSize
        element.fontName = fontName
        element.updateBounds()
        setNeedsDisplay()
    }

    func clearElementsForCurrentPage() {
        textElements.removeAll()
        signatureElements.removeAll()
        selectedTextElement = nil
        selectedSignatureElement = nil
        setNeedsDisplay()
    }
}

protocol EditablePDFViewDelegate: AnyObject {
    func editablePDFView(_ view: EditablePDFView, didDoubleTapTextElement element: TextElement)
}
