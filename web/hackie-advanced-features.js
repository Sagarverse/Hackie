/**
 * Hackie Advanced Features Module
 * Handles: Drag-drop file management, selected files preview,
 * transfer history tracking, and offline queue management.
 * 
 * All UI element access is null-safe to prevent runtime crashes.
 */

export class HackieAdvancedFeatures {
    constructor(uiElements = {}) {
        // UI Elements (all null-safe)
        this.dragDropZone = uiElements.dragDropZone || null;
        this.selectedFilesPreview = uiElements.selectedFilesPreview || null;
        this.clearFilesBtn = uiElements.clearFilesBtn || null;
        this.uploadInput = uiElements.uploadInput || null;
        this.sendFilesBtn = uiElements.sendFilesBtn || null;
        this.noteInputField = uiElements.noteInputField || null;
        this.dataChannel = null;
        this.showStatus = uiElements.showStatus || (() => {});
        this.formatSize = uiElements.formatSize || ((b) => `${b} B`);

        // State
        this.selectedFiles = [];
        this.transferHistoryLog = [];
        this.offlineQueueItems = [];
    }

    // ===== DRAG & DROP FILE UPLOAD =====
    setupDragDrop() {
        if (!this.dragDropZone || !this.uploadInput) return;

        this.dragDropZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            this.dragDropZone.style.background = 'rgba(10,132,255,0.15)';
            this.dragDropZone.style.borderColor = 'var(--accent-blue)';
        });

        this.dragDropZone.addEventListener('dragleave', () => {
            this.dragDropZone.style.background = 'rgba(10,132,255,0.05)';
            this.dragDropZone.style.borderColor = 'var(--border)';
        });

        this.dragDropZone.addEventListener('drop', (e) => {
            e.preventDefault();
            this.dragDropZone.style.background = 'rgba(10,132,255,0.05)';
            this.dragDropZone.style.borderColor = 'var(--border)';
            this.addSelectedFiles(Array.from(e.dataTransfer.files));
        });

        this.dragDropZone.addEventListener('click', () => this.uploadInput.click());

        if (this.selectedFilesPreview) {
            this.selectedFilesPreview.addEventListener('click', (event) => {
                const button = event.target.closest('[data-remove-index]');
                if (!button) return;
                const idx = Number(button.getAttribute('data-remove-index'));
                if (Number.isFinite(idx)) this.removeSelectedFile(idx);
            });
        }
    }

    addSelectedFiles(files) {
        if (!Array.isArray(files) || !files.length) return;
        this.selectedFiles.push(...files);
        this.renderSelectedFilesPreview();
    }

    removeSelectedFile(index) {
        if (index < 0 || index >= this.selectedFiles.length) return;
        this.selectedFiles.splice(index, 1);
        this.renderSelectedFilesPreview();
    }

    renderSelectedFilesPreview() {
        if (!this.selectedFilesPreview) return;

        if (!this.selectedFiles.length) {
            this.selectedFilesPreview.innerHTML = '';
            return;
        }

        this.selectedFilesPreview.innerHTML = this.selectedFiles.map((f, i) => `
            <div style="display:flex; justify-content:space-between; align-items:center; padding:8px 10px; background:rgba(10,132,255,0.08); border-radius:8px; font-size:11px; gap:10px;">
                <span>📄 ${f.name}</span>
                <span style="opacity:0.6; font-size:10px;">${this.formatSize(f.size)}</span>
                <button data-remove-index="${i}" style="border:none; background:transparent; color:#FF453A; cursor:pointer; padding:0; font-size:12px;">✕</button>
            </div>
        `).join('');
    }

    getSelectedFiles() {
        return this.selectedFiles;
    }

    clearSelectedFiles() {
        this.selectedFiles = [];
        this.renderSelectedFilesPreview();
    }

    // ===== TRANSFER HISTORY =====
    addToTransferHistory(type, content) {
        const entry = {
            type,
            content: (content + '').substring(0, 50),
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        };
        this.transferHistoryLog.unshift(entry);
        if (this.transferHistoryLog.length > 20) this.transferHistoryLog.pop();
    }

    // ===== OFFLINE SYNC QUEUE =====
    addToOfflineQueue(item) {
        this.offlineQueueItems.push({
            ...item,
            ts: Date.now(),
            id: Math.random().toString(36)
        });
    }

    retryOfflineQueue() {
        if (!this.dataChannel || this.dataChannel.readyState !== 'open') {
            return;
        }

        let syncedCount = 0;
        const failedItems = [];

        this.offlineQueueItems.forEach(item => {
            try {
                if (item.type === 'text') {
                    this.dataChannel.send(JSON.stringify({
                        type: 'ADD_NOTE',
                        text: item.content,
                        source: 'Web',
                        ts: Date.now()
                    }));
                    syncedCount++;
                } else if (item.type === 'file') {
                    syncedCount++;
                }
            } catch (e) {
                failedItems.push(item);
            }
        });

        this.offlineQueueItems = failedItems;

        if (syncedCount > 0) {
            this.showStatus(`Synced ${syncedCount} queued item(s).`);
        }
    }

    // ===== INITIALIZATION =====
    init() {
        this.setupDragDrop();
        this.renderSelectedFilesPreview();
    }

    setDataChannel(dc) {
        this.dataChannel = dc;
    }
}

export default HackieAdvancedFeatures;
