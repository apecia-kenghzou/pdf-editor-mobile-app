import UIKit
import PDFKit
import UniformTypeIdentifiers

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

    // Edit toolbar (appears when element is selected)
    private let editToolbar: UIView = {
        let view = UIView()
        view.backgroundColor = .systemBlue
        view.translatesAutoresizingMaskIntoConstraints = false
        view.isHidden = true
        return view
    }()

    private let addTextButton = UIButton(type: .system)
    private let addSignatureButton = UIButton(type: .system)
    private let removePageButton = UIButton(type: .system)
    private let saveButton = UIButton(type: .system)

    private let editButton = UIButton(type: .system)
    private let deleteButton = UIButton(type: .system)

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

        // Setup toolbars
        view.addSubview(toolbar)
        view.addSubview(editToolbar)
        setupToolbarButtons()
        setupEditToolbar()

        // Setup bottom toolbar
        view.addSubview(bottomToolbar)
        setupBottomToolbar()

        NSLayoutConstraint.activate([
            toolbar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            toolbar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            toolbar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            toolbar.heightAnchor.constraint(equalToConstant: 50),

            editToolbar.topAnchor.constraint(equalTo: toolbar.bottomAnchor),
            editToolbar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            editToolbar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            editToolbar.heightAnchor.constraint(equalToConstant: 50),

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

    private func setupEditToolbar() {
        editButton.setTitle("Edit", for: .normal)
        editButton.setTitleColor(.white, for: .normal)
        editButton.titleLabel?.font = UIFont.systemFont(ofSize: 14)
        editButton.addTarget(self, action: #selector(editTapped), for: .touchUpInside)

        deleteButton.setTitle("Delete", for: .normal)
        deleteButton.setTitleColor(.white, for: .normal)
        deleteButton.titleLabel?.font = UIFont.systemFont(ofSize: 14)
        deleteButton.addTarget(self, action: #selector(deleteTapped), for: .touchUpInside)

        let stackView = UIStackView(arrangedSubviews: [editButton, deleteButton])
        stackView.axis = .horizontal
        stackView.distribution = .fillEqually
        stackView.translatesAutoresizingMaskIntoConstraints = false

        editToolbar.addSubview(stackView)
        NSLayoutConstraint.activate([
            stackView.topAnchor.constraint(equalTo: editToolbar.topAnchor),
            stackView.leadingAnchor.constraint(equalTo: editToolbar.leadingAnchor),
            stackView.trailingAnchor.constraint(equalTo: editToolbar.trailingAnchor),
            stackView.bottomAnchor.constraint(equalTo: editToolbar.bottomAnchor)
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

        let picker = UIDocumentPickerViewController(forExporting: [pdfURL], asCopy: true)
        picker.delegate = self
        present(picker, animated: true)
    }

    @objc private func editTapped() {
        if let textElement = pdfView.getSelectedTextElement() {
            showTextEditDialog(element: textElement)
        } else if let signatureElement = pdfView.getSelectedSignatureElement() {
            showSignatureEditDialog(element: signatureElement)
        }
    }

    @objc private func deleteTapped() {
        pdfView.removeSelectedElement()
        updateEditToolbarVisibility()
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
                pdfView.textElements.append(element)
            }
        }

        if let signatureElements = pageSignatureElements[pageIndex] {
            for element in signatureElements {
                pdfView.signatureElements.append(element)
            }
        }

        pdfView.setNeedsDisplay()
        updateEditToolbarVisibility()
    }

    private func updatePageLabel() {
        guard let currentPage = pdfView.currentPage,
              let pageIndex = pdfDocument?.index(for: currentPage),
              let pageCount = pdfDocument?.pageCount else { return }

        pageLabel.text = "\(pageIndex + 1) / \(pageCount)"
    }

    private func updateEditToolbarVisibility() {
        editToolbar.isHidden = !pdfView.hasSelectedElement()
    }

    private func showTextEditDialog(element: TextElement?) {
        let alert = UIAlertController(title: element == nil ? "Add Text" : "Edit Text",
                                      message: nil,
                                      preferredStyle: .alert)

        alert.addTextField { textField in
            textField.placeholder = "Enter text"
            textField.text = element?.text
        }

        alert.addTextField { textField in
            textField.placeholder = "Font size (e.g., 12)"
            textField.keyboardType = .numberPad
            textField.text = element != nil ? String(format: "%.0f", element!.fontSize) : "12"
        }

        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))

        // Add delete button only when editing
        if element != nil {
            alert.addAction(UIAlertAction(title: "Delete", style: .destructive) { [weak self] _ in
                self?.pdfView.removeSelectedElement()
                self?.updateEditToolbarVisibility()
            })
        }

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
                let center = self.pdfView.getPdfCenter()
                let newElement = TextElement(text: text,
                                           position: center,
                                           fontSize: fontSize,
                                           fontName: "Helvetica")
                self.pdfView.addTextElement(newElement)
            }
            self.updateEditToolbarVisibility()
        })

        present(alert, animated: true)
    }

    private func showSignatureEditDialog(element: SignatureElement) {
        let alert = UIAlertController(title: "Edit Signature",
                                      message: nil,
                                      preferredStyle: .alert)

        alert.addTextField { textField in
            textField.placeholder = "Width (e.g., 200)"
            textField.keyboardType = .numberPad
            textField.text = String(format: "%.0f", element.size.width)
        }

        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))

        alert.addAction(UIAlertAction(title: "Delete", style: .destructive) { [weak self] _ in
            self?.pdfView.removeSelectedElement()
            self?.updateEditToolbarVisibility()
        })

        alert.addAction(UIAlertAction(title: "OK", style: .default) { [weak self, weak alert] _ in
            guard let self = self,
                  let widthField = alert?.textFields?[0],
                  let widthText = widthField.text,
                  let newWidth = CGFloat(widthText) else { return }

            let aspectRatio = element.image.size.width / element.image.size.height
            let newHeight = newWidth / aspectRatio
            let newSize = CGSize(width: newWidth, height: newHeight)

            self.pdfView.updateSignatureElement(element, size: newSize)
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

    func editablePDFView(_ view: EditablePDFView, didDoubleTapSignatureElement element: SignatureElement) {
        showSignatureEditDialog(element: element)
    }

    func editablePDFViewSelectionChanged(_ view: EditablePDFView) {
        updateEditToolbarVisibility()
    }
}

extension PDFEditorViewController: SignatureViewControllerDelegate {
    func signatureViewController(_ controller: SignatureViewController, didSelectSignature image: UIImage) {
        let center = pdfView.getPdfCenter()
        let aspectRatio = image.size.width / image.size.height
        let targetWidth: CGFloat = 200
        let targetHeight = targetWidth / aspectRatio
        let size = CGSize(width: targetWidth, height: targetHeight)
        let position = CGPoint(x: center.x - size.width / 2, y: center.y - size.height / 2)

        let signatureElement = SignatureElement(image: image, position: position, size: size)
        pdfView.addSignatureElement(signatureElement)
        updateEditToolbarVisibility()
    }
}

extension PDFEditorViewController: UIDocumentPickerDelegate {
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        // Document saved successfully
        showAlert(message: "PDF saved successfully!")
    }
}
