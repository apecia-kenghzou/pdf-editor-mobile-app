import UIKit

protocol SignatureViewControllerDelegate: AnyObject {
    func signatureViewController(_ controller: SignatureViewController, didSelectSignature image: UIImage)
}

class SignatureViewController: UIViewController {

    weak var delegate: SignatureViewControllerDelegate?

    private let signatureView = SignatureDrawingView()
    private let savedSignaturesCollectionView: UICollectionView
    private let signatureManager = SignatureManager.shared
    private var savedSignatures: [UIImage] = []

    private let titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Draw Your Signature"
        label.font = UIFont.boldSystemFont(ofSize: 20)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let clearButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Clear", for: .normal)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    private let saveButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Save Signature", for: .normal)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    private let useButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("Use", for: .normal)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    private let savedLabel: UILabel = {
        let label = UILabel()
        label.text = "Saved Signatures"
        label.font = UIFont.boldSystemFont(ofSize: 16)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    init() {
        let layout = UICollectionViewFlowLayout()
        layout.scrollDirection = .horizontal
        layout.itemSize = CGSize(width: 150, height: 100)
        savedSignaturesCollectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadSavedSignatures()
    }

    private func setupUI() {
        view.backgroundColor = .systemBackground

        signatureView.translatesAutoresizingMaskIntoConstraints = false
        savedSignaturesCollectionView.translatesAutoresizingMaskIntoConstraints = false
        savedSignaturesCollectionView.backgroundColor = .systemGray6
        savedSignaturesCollectionView.delegate = self
        savedSignaturesCollectionView.dataSource = self
        savedSignaturesCollectionView.register(SignatureCell.self, forCellWithReuseIdentifier: "SignatureCell")

        view.addSubview(titleLabel)
        view.addSubview(signatureView)
        view.addSubview(clearButton)
        view.addSubview(saveButton)
        view.addSubview(useButton)
        view.addSubview(savedLabel)
        view.addSubview(savedSignaturesCollectionView)

        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 16),
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),

            signatureView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 16),
            signatureView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            signatureView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            signatureView.heightAnchor.constraint(equalToConstant: 200),

            clearButton.topAnchor.constraint(equalTo: signatureView.bottomAnchor, constant: 16),
            clearButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            clearButton.widthAnchor.constraint(equalTo: view.widthAnchor, multiplier: 0.28),

            saveButton.topAnchor.constraint(equalTo: signatureView.bottomAnchor, constant: 16),
            saveButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            saveButton.widthAnchor.constraint(equalTo: view.widthAnchor, multiplier: 0.28),

            useButton.topAnchor.constraint(equalTo: signatureView.bottomAnchor, constant: 16),
            useButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            useButton.widthAnchor.constraint(equalTo: view.widthAnchor, multiplier: 0.28),

            savedLabel.topAnchor.constraint(equalTo: clearButton.bottomAnchor, constant: 24),
            savedLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),

            savedSignaturesCollectionView.topAnchor.constraint(equalTo: savedLabel.bottomAnchor, constant: 8),
            savedSignaturesCollectionView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            savedSignaturesCollectionView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            savedSignaturesCollectionView.heightAnchor.constraint(equalToConstant: 120)
        ])

        clearButton.addTarget(self, action: #selector(clearTapped), for: .touchUpInside)
        saveButton.addTarget(self, action: #selector(saveTapped), for: .touchUpInside)
        useButton.addTarget(self, action: #selector(useTapped), for: .touchUpInside)
    }

    private func loadSavedSignatures() {
        savedSignatures = signatureManager.getSavedSignatures()
        savedSignaturesCollectionView.reloadData()
    }

    @objc private func clearTapped() {
        signatureView.clear()
    }

    @objc private func saveTapped() {
        guard !signatureView.isEmpty(), let image = signatureView.getSignatureImage() else {
            showAlert(message: "Please draw a signature first")
            return
        }

        signatureManager.saveSignature(image)
        showAlert(message: "Signature saved")
        loadSavedSignatures()
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

extension SignatureViewController: UICollectionViewDelegate, UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return savedSignatures.count
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "SignatureCell", for: indexPath) as! SignatureCell
        cell.imageView.image = savedSignatures[indexPath.item]
        return cell
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let image = savedSignatures[indexPath.item]
        delegate?.signatureViewController(self, didSelectSignature: image)
        dismiss(animated: true)
    }
}

class SignatureCell: UICollectionViewCell {
    let imageView: UIImageView = {
        let iv = UIImageView()
        iv.contentMode = .scaleAspectFit
        iv.backgroundColor = .white
        iv.layer.borderColor = UIColor.black.cgColor
        iv.layer.borderWidth = 1
        iv.translatesAutoresizingMaskIntoConstraints = false
        return iv
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.addSubview(imageView)
        NSLayoutConstraint.activate([
            imageView.topAnchor.constraint(equalTo: contentView.topAnchor),
            imageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            imageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
            imageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor)
        ])
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
