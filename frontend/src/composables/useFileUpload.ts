import { ref } from 'vue'
import { uploadFiles } from '../api/rag'

const VALID_FILE_TYPES = ['.pdf', '.doc', '.docx', '.txt', '.md', '.xls', '.xlsx']

export function useFileUpload() {
  const files = ref<File[]>([])
  const uploading = ref(false)
  const uploadModalOpen = ref(false)
  const isDragOver = ref(false)
  const fileInputRef = ref<HTMLInputElement | null>(null)

  function validateFile(file: File): boolean {
    return VALID_FILE_TYPES.some(type => file.name.toLowerCase().endsWith(type))
  }

  function handleFileSelect(selectedFiles: FileList | null): void {
    if (!selectedFiles) return
    const validFiles = Array.from(selectedFiles).filter(validateFile)
    if (validFiles.length > 0) {
      files.value = validFiles
    }
  }

  function onDragOver(_e: DragEvent): void {
    _e.preventDefault()
    _e.stopPropagation()
    isDragOver.value = true
  }

  function onDragLeave(_e: DragEvent): void {
    _e.preventDefault()
    _e.stopPropagation()
    isDragOver.value = false
  }

  function onDrop(e: DragEvent): void {
    e.preventDefault()
    e.stopPropagation()
    isDragOver.value = false
    handleFileSelect(e.dataTransfer?.files || null)
  }

  function triggerFileInput(): void {
    fileInputRef.value?.click()
  }

  async function doUpload(
    showToast: (text: string, type?: 'success' | 'error') => void
  ): Promise<void> {
    if (files.value.length === 0 || uploading.value) return
    uploading.value = true
    try {
      await uploadFiles(files.value)
      showToast('上传成功', 'success')
      files.value = []
      uploadModalOpen.value = false
    } catch (error) {
      showToast(error instanceof Error ? error.message : '上传失败', 'error')
    } finally {
      uploading.value = false
    }
  }

  function closeUploadModal(): void {
    uploadModalOpen.value = false
    files.value = []
    isDragOver.value = false
  }

  return {
    files,
    uploading,
    uploadModalOpen,
    isDragOver,
    fileInputRef,
    handleFileSelect,
    onDragOver,
    onDragLeave,
    onDrop,
    triggerFileInput,
    doUpload,
    closeUploadModal
  }
}
