window.customElements.define('custom-file-upload', class extends HTMLElement {
    constructor() {
        super();

        // Create shadow DOM
        this.attachShadow({mode: 'open'});

        // Add styles
        const style = document.createElement('style');
        style.textContent = `
            .upload-container {
                border: 2px dashed #ccc;
                border-radius: 4px;
                padding: 20px;
                text-align: center;
                background: #f9f9f9;
                cursor: pointer;
                transition: all 0.3s ease;
            }

            .upload-container:hover {
                border-color: #666;
                background: #f0f0f0;
            }

            .upload-container.dragover {
                border-color: #000;
                background: #e9e9e9;
            }

            input[type="file"] {
                display: none;
            }

            .upload-label {
                color: #666;
                font-family: var(--lumo-font-family);
                font-size: var(--lumo-font-size-m);
            }

            .files-list {
                margin-top: 10px;
                text-align: left;
            }

            .file-item {
                font-size: 0.9em;
                color: #666;
                margin: 5px 0;
            }

            .remove-file {
                color: #f44336;
                margin-left: 5px;
                cursor: pointer;
            }
        `;

        // Create content
        const container = document.createElement('div');
        container.className = 'upload-container';

        const input = document.createElement('input');
        input.type = 'file';
        input.id = 'fileInput';
        input.multiple = true; // Allow multiple file selection

        if (this.hasAttribute('accept')) {
            input.accept = this.getAttribute('accept');
        } else {
            input.accept = '.json'; // Default to JSON files
        }

        const label = document.createElement('div');
        label.className = 'upload-label';
        label.textContent = 'Перетащите файлы postman collection с тестовыми сценариями сюда или нажмите для выбора';

        const filesList = document.createElement('div');
        filesList.className = 'files-list';

        container.appendChild(input);
        container.appendChild(label);
        container.appendChild(filesList);

        this.shadowRoot.append(style, container);

        // Store selected files
        this.selectedFiles = [];

        // Event listeners
        container.addEventListener('click', (e) => {
            // Prevent clicking on remove buttons from triggering file selection
            if (e.target.className !== 'remove-file') {
                input.click();
            }
        });

        container.addEventListener('dragover', (e) => {
            e.preventDefault();
            container.classList.add('dragover');
        });

        container.addEventListener('dragleave', () => {
            container.classList.remove('dragover');
        });

        container.addEventListener('drop', (e) => {
            e.preventDefault();
            container.classList.remove('dragover');
            handleFiles(Array.from(e.dataTransfer.files));
        });

        input.addEventListener('change', (e) => {
            handleFiles(Array.from(e.target.files));
        });

        const handleFiles = (files) => {
            if (files.length === 0) return;

            // Limit to 5 files total
            if (this.selectedFiles.length + files.length > 5) {
                alert('Можно загрузить максимум 5 файлов');
                return;
            }

            // Filter only JSON files
            const jsonFiles = files.filter(file => file.name.toLowerCase().endsWith('.json'));

            if (jsonFiles.length === 0) {
                // FIX: Use native alert to ensure it works
                alert('Пожалуйста, выберите JSON файлы');
                return;
            }

            if (jsonFiles.length !== files.length) {
                // FIX: Use native alert to ensure it works
                alert('Некоторые файлы были пропущены, так как они не в формате JSON');
            }

            // Array to store files that will be processed
            const filesToProcess = [];
            const duplicates = [];

            // Process each file
            for (const file of jsonFiles) {
                // FIX: Check for duplicates by filename
                const isDuplicate = this.selectedFiles.some(existingFile => existingFile.name === file.name);

                if (isDuplicate) {
                    duplicates.push(file.name);
                } else {
                    filesToProcess.push(file);
                }
            }

            // Notify about duplicates if any
            if (duplicates.length > 0) {
                alert(`Следующие файлы уже загружены и были пропущены: ${duplicates.join(', ')}`);
            }

            // Process non-duplicate files
            for (const file of filesToProcess) {
                const reader = new FileReader();
                reader.onload = (e) => {
                    // Add to selected files array
                    this.selectedFiles.push({
                        name: file.name,
                        base64Data: e.target.result
                    });

                    // Update UI
                    updateFilesList();

                    // Dispatch event with all files after each file is processed
                    // to ensure server-side component has updated data
          this.dispatchEvent(new CustomEvent('files-selected', {
              detail: {
                  files: this.selectedFiles.map(file => ({
                      name: file.name,
                      base64Data: file.base64Data
                  }))
              }
          }));
                };
                reader.readAsDataURL(file);
            }
        };

        const updateFilesList = () => {
            // Clear the list
            filesList.innerHTML = '';

            // Add each file to the list
            this.selectedFiles.forEach((file, index) => {
                const fileItem = document.createElement('div');
                fileItem.className = 'file-item';
                fileItem.textContent = file.name;

                const removeButton = document.createElement('span');
                removeButton.className = 'remove-file';
                removeButton.textContent = '✕';
                removeButton.addEventListener('click', (e) => {
                    e.stopPropagation(); // Prevent triggering container click
                    this.selectedFiles.splice(index, 1);
                    updateFilesList();

                    // Dispatch updated files event
          this.dispatchEvent(new CustomEvent('files-selected', {
              detail: {
                  files: this.selectedFiles.map(file => ({
                      name: file.name,
                      base64Data: file.base64Data
                  }))
              }
          }));
                });

                fileItem.appendChild(removeButton);
                filesList.appendChild(fileItem);
            });

            // Update label visibility
            if (this.selectedFiles.length > 0) {
                label.textContent = 'Добавить больше файлов';
            } else {
                label.textContent = 'Перетащите файлы postman collection с тестовыми сценариями сюда или нажмите для выбора';
            }

            // Disable file selection if 5 files are already selected
            if (this.selectedFiles.length >= 5) {
                label.textContent = 'Максимальное количество файлов (5) загружено';
                container.style.cursor = 'not-allowed';
                container.style.opacity = '0.6';
            } else {
                container.style.cursor = 'pointer';
                container.style.opacity = '1';
            }
        };
    }

    static get observedAttributes() {
        return ['accept'];
    }

    attributeChangedCallback(name, oldValue, newValue) {
        if (name === 'accept') {
            const input = this.shadowRoot.querySelector('input');
            if (input) {
                input.accept = newValue;
            }
        }
    }

    clearFiles() {
       // Полная очистка файлов
        this.selectedFiles = [];

        // Очистка input
        const input = this.shadowRoot.querySelector('input');
        input.value = '';

        // Очистка списка файлов
        const filesList = this.shadowRoot.querySelector('.files-list');
        filesList.innerHTML = '';

        // Возвращение label к исходному состоянию
        const label = this.shadowRoot.querySelector('.upload-label');
        label.textContent = 'Перетащите файлы postman collection с тестовыми сценариями сюда или нажмите для выбора';

        // Сброс стилей контейнера
        const container = this.shadowRoot.querySelector('.upload-container');
        container.style.cursor = 'pointer';
        container.style.opacity = '1';

        // Диспетчеризация события об очистке файлов
        this.dispatchEvent(new CustomEvent('files-selected', {
            detail: {
                files: []
            }
        }));
    }

    getSelectedFiles() {
        return this.selectedFiles;
    }
});