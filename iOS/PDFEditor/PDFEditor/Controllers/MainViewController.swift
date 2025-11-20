import UIKit
import PDFKit
import UniformTypeIdentifiers

class MainViewController: UIViewController {

    private let titleLabel: UILabel = {
        let label = UILabel()
        label.text = "PDF Editor"
        label.font = UIFont.boldSystemFont(ofSize: 32)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let selectButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Select PDF to Edit", for: .normal)
        button.titleLabel?.font = UIFont.systemFont(ofSize: 18)
        button.backgroundColor = .systemBlue
        button.setTitleColor(.white, for: .normal)
        button.layer.cornerRadius = 10
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    private let featuresLabel: UILabel = {
        let label = UILabel()
        label.text = """
        Features:
        • Add and edit text
        • Move text around
        • Change font size and type
        • Draw and save signatures
        • Remove pages
        • Save to new PDF
        """
        label.numberOfLines = 0
        label.font = UIFont.systemFont(ofSize: 16)
        label.textAlignment = .left
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }

    private func setupUI() {
        view.backgroundColor = .systemBackground

        view.addSubview(titleLabel)
        view.addSubview(selectButton)
        view.addSubview(featuresLabel)

        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 60),
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),

            selectButton.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 40),
            selectButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 32),
            selectButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -32),
            selectButton.heightAnchor.constraint(equalToConstant: 50),

            featuresLabel.topAnchor.constraint(equalTo: selectButton.bottomAnchor, constant: 60),
            featuresLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 32),
            featuresLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -32)
        ])

        selectButton.addTarget(self, action: #selector(selectPDFTapped), for: .touchUpInside)
    }

    @objc private func selectPDFTapped() {
        let documentPicker = UIDocumentPickerViewController(forOpeningContentTypes: [UTType.pdf])
        documentPicker.delegate = self
        documentPicker.allowsMultipleSelection = false
        present(documentPicker, animated: true)
    }
}

extension MainViewController: UIDocumentPickerDelegate {
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        guard let url = urls.first else { return }

        // Start accessing the security-scoped resource
        guard url.startAccessingSecurityScopedResource() else {
            showAlert(message: "Cannot access the selected file")
            return
        }

        defer { url.stopAccessingSecurityScopedResource() }

        // Open PDF Editor
        let editorVC = PDFEditorViewController(pdfURL: url)
        navigationController?.pushViewController(editorVC, animated: true)
    }

    private func showAlert(message: String) {
        let alert = UIAlertController(title: "Error", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}
