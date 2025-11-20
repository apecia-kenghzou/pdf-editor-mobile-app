import UIKit

class SignatureDrawingView: UIView {

    private var path = UIBezierPath()
    private var previousPoint: CGPoint?
    private var image: UIImage?

    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setup()
    }

    private func setup() {
        backgroundColor = .white
        layer.borderColor = UIColor.black.cgColor
        layer.borderWidth = 1
    }

    override func draw(_ rect: CGRect) {
        super.draw(rect)

        image?.draw(in: rect)
        UIColor.black.setStroke()
        path.lineWidth = 3.0
        path.lineCapStyle = .round
        path.lineJoinStyle = .round
        path.stroke()
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }
        previousPoint = touch.location(in: self)
        path.move(to: previousPoint!)
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = touches.first else { return }
        let currentPoint = touch.location(in: self)

        path.addLine(to: currentPoint)
        previousPoint = currentPoint

        setNeedsDisplay()
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        updateImage()
    }

    private func updateImage() {
        UIGraphicsBeginImageContextWithOptions(bounds.size, false, 0.0)
        if let context = UIGraphicsGetCurrentContext() {
            // Draw existing image
            image?.draw(in: bounds)

            // Draw new path
            context.setStrokeColor(UIColor.black.cgColor)
            context.setLineWidth(3.0)
            context.setLineCap(.round)
            context.setLineJoin(.round)
            path.stroke()

            image = UIGraphicsGetImageFromCurrentImageContext()
        }
        UIGraphicsEndImageContext()

        path.removeAllPoints()
        setNeedsDisplay()
    }

    func clear() {
        path.removeAllPoints()
        image = nil
        setNeedsDisplay()
    }

    func getSignatureImage() -> UIImage? {
        return image
    }

    func isEmpty() -> Bool {
        return image == nil
    }
}
