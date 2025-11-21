import UIKit

protocol SignatureViewControllerDelegate: AnyObject {
    func signatureViewController(_ controller: SignatureViewController, didSelectSignature image: UIImage)
}

class SignatureViewController: UIViewController {

    weak var delegate: SignatureViewControllerDelegate?

    private let signatureView = SignatureDrawingView()

    private let titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Draw Your Signature"
        label.font = UIFont.boldSystemFont(ofSize: 24)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let clearButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Clear", for: .normal)
        button.titleLabel?.font = UIFont.systemFont(ofSize: 16)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    private let useButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Use Signature", for: .normal)
        button.titleLabel?.font = UIFont.systemFont(ofSize: 16)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }

    private func setupUI() {
        view.backgroundColor = .systemBackground

        signatureView.translatesAutoresizingMaskIntoConstraints = false
        signatureView.backgroundColor = .white
        signatureView.layer.shadowColor = UIColor.black.cgColor
        signatureView.layer.shadowOffset = CGSize(width: 0, height: 2)
        signatureView.layer.shadowRadius = 4
        signatureView.layer.shadowOpacity = 0.2

        view.addSubview(titleLabel)
        view.addSubview(signatureView)
        view.addSubview(clearButton)
        view.addSubview(useButton)

        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 24),
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),

            signatureView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 24),
            signatureView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            signatureView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            signatureView.bottomAnchor.constraint(equalTo: clearButton.topAnchor, constant: -24),

            clearButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            clearButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -16),
            clearButton.widthAnchor.constraint(equalTo: view.widthAnchor, multiplier: 0.45),
            clearButton.heightAnchor.constraint(equalToConstant: 50),

            useButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            useButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -16),
            useButton.widthAnchor.constraint(equalTo: view.widthAnchor, multiplier: 0.45),
            useButton.heightAnchor.constraint(equalToConstant: 50)
        ])

        clearButton.addTarget(self, action: #selector(clearTapped), for: .touchUpInside)
        useButton.addTarget(self, action: #selector(useTapped), for: .touchUpInside)
    }

    @objc private func clearTapped() {
        signatureView.clear()
    }

    @objc private func useTapped() {
        guard !signatureView.isEmpty(), let image = signatureView.getSignatureImage() else {
            showAlert(message: "Please draw a signature first")
            return
        }

        delegate?.signatureViewController(self, didSelectSignature: image)
        dismiss(animated: true)
    }

    private func showAlert(message: String) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}
