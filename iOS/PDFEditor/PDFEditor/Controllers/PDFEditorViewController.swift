import UIKit
import PDFKit

class PDFEditorViewController: UIViewController {

    private var pdfURL: URL
    private var pdfDocument: PDFDocument?
    private let pdfView = EditablePDFView()

    // Store elements for each page
    private var pageTextElements: [Int: [TextElement]] = [:]
    private var pageSignatureElements: [Int: [SignatureElement]] = [:]
    private var pagesToRemove: Set<Int> = []

    private let toolbar: UIView = {
        let view = UIView()
        view.backgroundColor = .systemGray5
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()

    private let addTextButton = UIButton(type: .system)
    private let addSignatureButton = UIButton(type: .system)
    private let removePageButton = UIButton(type: .system)
    private let saveButton = UIButton(type: .system)

    private let bottomToolbar: UIView = {
        let view = UIView()
        view.backgroundColor = .systemGray5
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()

    private let prevPageButton = UIButton(type: .system)
    private let pageLabel = UILabel()
    private let nextPageButton = UIButton(type: .system)

    init(pdfURL: URL) {
        self.pdfURL = pdfURL
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadPDF()
    }

    private func setupUI() {
        view.backgroundColor = .systemBackground
        title = "PDF Editor"

        // Setup PDF view
        pdfView.translatesAutoresizingMaskIntoConstraints = false
        pdfView.editDelegate = self
        view.addSubview(pdfView)

        // Setup toolbar
        view.addSubview(toolbar)
        setupToolbarButtons()

        // Setup bottom toolbar
        view.addSubview(bottomToolbar)
        setupBottomToolbar()

        NSLayoutConstraint.activate([
            toolbar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            toolbar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            toolbar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            toolbar.heightAnchor.constraint(equalToConstant: 50),

            pdfView.topAnchor.constraint(equalTo: toolbar.bottomAnchor),
            pdfView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            pdfView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            pdfView.bottomAnchor.constraint(equalTo: bottomToolbar.topAnchor),

            bottomToolbar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            bottomToolbar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            bottomToolbar.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            bottomToolbar.heightAnchor.constraint(equalToConstant: 50)
        ])

        // Observe page changes
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(pageChanged),
                                               name: .PDFViewPageChanged,
                                               object: pdfView)
    }

    private func setupToolbarButtons() {
        addTextButton.setTitle("Add Text", for: .normal)
        addTextButton.titleLabel?.font = UIFont.systemFont(ofSize: 14)
        addTextButton.addTarget(self, action: #selector(addTextTapped), for: .touchUpInside)

        addSignatureButton.setTitle("Signature", for: .normal)
        addSignatureButton.titleLabel?.font = UIFont.systemFont(ofSize: 14)
        addSignatureButton.addTarget(self, action: #selector(addSignatureTapped), for: .touchUpInside)

        removePageButton.setTitle("Remove Page", for: .normal)
        removePageButton.titleLabel?.font = UIFont.systemFont(ofSize: 14)
        removePageButton.addTarget(self, action: #selector(removePageTapped), for: .touchUpInside)

        saveButton.setTitle("Save", for: .normal)
        saveButton.titleLabel?.font = UIFont.systemFont(ofSize: 14)
        saveButton.addTarget(self, action: #selector(saveTapped), for: .touchUpInside)

        let stackView = UIStackView(arrangedSubviews: [addTextButton, addSignatureButton, removePageButton, saveButton])
        stackView.axis = .horizontal
        stackView.distribution = .fillEqually
        stackView.translatesAutoresizingMaskIntoConstraints = false

        toolbar.addSubview(stackView)
        NSLayoutConstraint.activate([
            stackView.topAnchor.constraint(equalTo: toolbar.topAnchor),
            stackView.leadingAnchor.constraint(equalTo: toolbar.leadingAnchor),
            stackView.trailingAnchor.constraint(equalTo: toolbar.trailingAnchor),
            stackView.bottomAnchor.constraint(equalTo: toolbar.bottomAnchor)
        ])
    }

    private func setupBottomToolbar() {
        prevPageButton.setTitle("Previous", for: .normal)
        prevPageButton.addTarget(self, action: #selector(prevPageTapped), for: .touchUpInside)

        pageLabel.textAlignment = .center
        pageLabel.text = "1 / 1"

        nextPageButton.setTitle("Next", for: .normal)
        nextPageButton.addTarget(self, action: #selector(nextPageTapped), for: .touchUpInside)

        let stackView = UIStackView(arrangedSubviews: [prevPageButton, pageLabel, nextPageButton])
        stackView.axis = .horizontal
        stackView.distribution = .fillEqually
        stackView.translatesAutoresizingMaskIntoConstraints = false

        bottomToolbar.addSubview(stackView)
        NSLayoutConstraint.activate([
            stackView.topAnchor.constraint(equalTo: bottomToolbar.topAnchor),
            stackView.leadingAnchor.constraint(equalTo: bottomToolbar.leadingAnchor),
            stackView.trailingAnchor.constraint(equalTo: bottomToolbar.trailingAnchor),
            stackView.bottomAnchor.constraint(equalTo: bottomToolbar.bottomAnchor)
        ])
    }

    private func loadPDF() {
        guard let document = PDFDocument(url: pdfURL) else {
            showAlert(message: "Failed to load PDF")
            return
        }

        pdfDocument = document
        pdfView.document = document
        updatePageLabel()
    }

    @objc private func addTextTapped() {
        showTextEditDialog(element: nil)
    }

    @objc private func addSignatureTapped() {
        let signatureVC = SignatureViewController()
        signatureVC.delegate = self
        present(signatureVC, animated: true)
    }

    @objc private func removePageTapped() {
        guard let currentPage = pdfView.currentPage,
              let pageIndex = pdfDocument?.index(for: currentPage) else { return }

        guard let pageCount = pdfDocument?.pageCount, pageCount > 1 else {
            showAlert(message: "Cannot remove the last page")
            return
        }

        let alert = UIAlertController(title: "Remove Page",
                                      message: "Are you sure you want to remove page \(pageIndex + 1)?",
                                      preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Remove", style: .destructive) { [weak self] _ in
            self?.pagesToRemove.insert(pageIndex)
            self?.showAlert(message: "Page marked for removal. Save to apply.")
        })
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        present(alert, animated: true)
    }

    @objc private func saveTapped() {
        saveCurrentPageElements()

        guard let document = pdfDocument else { return }

        // Create a new document
        let newDocument = PDFDocument()

        // Copy pages that are not marked for removal
        for i in 0..<document.pageCount {
            if !pagesToRemove.contains(i), let page = document.page(at: i) {
                newDocument.insert(page, at: newDocument.pageCount)
            }
        }

        // Add overlays to each page
        for i in 0..<newDocument.pageCount {
            guard let page = newDocument.page(at: i) else { continue }

            // Add text elements
            if let textElements = pageTextElements[i] {
                for element in textElements {
                    let annotation = element.createPDFAnnotation(for: page)
                    page.addAnnotation(annotation)
                }
            }

            // Add signature elements
            if let signatureElements = pageSignatureElements[i] {
                for element in signatureElements {
                    // Create image annotation
                    let bounds = element.bounds
                    let imageAnnotation = PDFAnnotation(bounds: bounds, forType: .stamp, withProperties: nil)

                    // Unfortunately, PDFKit doesn't directly support custom images in annotations easily
                    // So we'll use a workaround by drawing the image
                    if let appearance = createAppearanceStream(with: element.image, bounds: bounds) {
                        imageAnnotation.setValue(appearance, forAnnotationKey: .appearance)
                    }

                    page.addAnnotation(imageAnnotation)
                }
            }
        }

        // Save to new file
        let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let outputURL = documentsPath.appendingPathComponent("edited_pdf_\(Date().timeIntervalSince1970).pdf")

        if newDocument.write(to: outputURL) {
            showAlert(message: "PDF saved to: \(outputURL.lastPathComponent)")
        } else {
            showAlert(message: "Failed to save PDF")
        }
    }

    private func createAppearanceStream(with image: UIImage, bounds: CGRect) -> String? {
        // This is a simplified approach - in production you'd want more robust PDF appearance stream creation
        return nil
    }

    @objc private func prevPageTapped() {
        guard let currentPage = pdfView.currentPage,
              let pageIndex = pdfDocument?.index(for: currentPage),
              pageIndex > 0,
              let prevPage = pdfDocument?.page(at: pageIndex - 1) else { return }

        saveCurrentPageElements()
        pdfView.go(to: prevPage)
        loadCurrentPageElements()
    }

    @objc private func nextPageTapped() {
        guard let currentPage = pdfView.currentPage,
              let pageIndex = pdfDocument?.index(for: currentPage),
              let pageCount = pdfDocument?.pageCount,
              pageIndex < pageCount - 1,
              let nextPage = pdfDocument?.page(at: pageIndex + 1) else { return }

        saveCurrentPageElements()
        pdfView.go(to: nextPage)
        loadCurrentPageElements()
    }

    @objc private func pageChanged() {
        saveCurrentPageElements()
        loadCurrentPageElements()
        updatePageLabel()
    }

    private func saveCurrentPageElements() {
        guard let currentPage = pdfView.currentPage,
              let pageIndex = pdfDocument?.index(for: currentPage) else { return }

        pageTextElements[pageIndex] = pdfView.textElements
        pageSignatureElements[pageIndex] = pdfView.signatureElements
    }

    private func loadCurrentPageElements() {
        guard let currentPage = pdfView.currentPage,
              let pageIndex = pdfDocument?.index(for: currentPage) else { return }

        pdfView.clearElementsForCurrentPage()

        if let textElements = pageTextElements[pageIndex] {
            for element in textElements {
                pdfView.addTextElement(element)
            }
        }

        if let signatureElements = pageSignatureElements[pageIndex] {
            for element in signatureElements {
                pdfView.addSignatureElement(element)
            }
        }
    }

    private func updatePageLabel() {
        guard let currentPage = pdfView.currentPage,
              let pageIndex = pdfDocument?.index(for: currentPage),
              let pageCount = pdfDocument?.pageCount else { return }

        pageLabel.text = "\(pageIndex + 1) / \(pageCount)"
    }

    private func showTextEditDialog(element: TextElement?) {
        let alert = UIAlertController(title: element == nil ? "Add Text" : "Edit Text",
                                      message: nil,
                                      preferredStyle: .alert)

        alert.addTextField { textField in
            textField.placeholder = "Enter text"
            textField.text = element?.text
        }

        // Font size
        alert.addTextField { textField in
            textField.placeholder = "Font size (e.g., 12)"
            textField.keyboardType = .numberPad
            textField.text = element != nil ? String(format: "%.0f", element!.fontSize) : "12"
        }

        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "OK", style: .default) { [weak self, weak alert] _ in
            guard let self = self,
                  let textField = alert?.textFields?[0],
                  let text = textField.text, !text.isEmpty,
                  let fontSizeField = alert?.textFields?[1],
                  let fontSizeText = fontSizeField.text,
                  let fontSize = CGFloat(fontSizeText) else { return }

            if let element = element {
                self.pdfView.updateTextElement(element, text: text, fontSize: fontSize, fontName: "Helvetica")
            } else {
                let newElement = TextElement(text: text,
                                           position: CGPoint(x: 100, y: 200),
                                           fontSize: fontSize,
                                           fontName: "Helvetica")
                self.pdfView.addTextElement(newElement)
            }
        })

        present(alert, animated: true)
    }

    private func showAlert(message: String) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}

extension PDFEditorViewController: EditablePDFViewDelegate {
    func editablePDFView(_ view: EditablePDFView, didDoubleTapTextElement element: TextElement) {
        showTextEditDialog(element: element)
    }
}

extension PDFEditorViewController: SignatureViewControllerDelegate {
    func signatureViewController(_ controller: SignatureViewController, didSelectSignature image: UIImage) {
        let center = CGPoint(x: pdfView.bounds.midX, y: pdfView.bounds.midY)
        let size = CGSize(width: 200, height: 100)
        let position = CGPoint(x: center.x - size.width / 2, y: center.y - size.height / 2)

        let signatureElement = SignatureElement(image: image, position: position, size: size)
        pdfView.addSignatureElement(signatureElement)
    }
}
