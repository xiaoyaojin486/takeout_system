<template>
  <div class="image-upload">
    <el-upload
      class="uploader"
      :show-file-list="false"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      accept="image/jpeg,image/png,image/gif,image/webp"
    >
      <img v-if="modelValue" :src="previewUrl" class="uploaded-image" alt="图片" />
      <div v-else class="upload-placeholder">
        <el-icon :size="24"><Plus /></el-icon>
        <span class="placeholder-text">上传图片</span>
      </div>
    </el-upload>
    <div class="upload-tips">
      <span class="text-muted">支持 jpg/png/gif/webp，≤5MB</span>
      <el-button
        v-if="modelValue"
        link
        type="danger"
        size="small"
        @click="handleRemove"
      >
        删除图片
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'
import { resolveImageUrl } from '@/utils/image'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const loading = ref(false)

const previewUrl = computed(() => resolveImageUrl(props.modelValue))

/** 上传前置校验：格式 + 大小 */
function beforeUpload(file) {
  const allowTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg / jpeg / png / gif / webp 格式的图片')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

/** 自定义上传：调用 /file/upload，回填返回的 url（不带 /api 前缀存储） */
async function handleUpload(options) {
  loading.value = true
  try {
    const data = await uploadFile(options.file)
    emit('update:modelValue', data?.url || '')
    emit('success', data)
    options.onSuccess?.(data)
    ElMessage.success('图片上传成功')
  } catch (error) {
    options.onError?.(error)
  } finally {
    loading.value = false
  }
}

function handleRemove() {
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-upload {
  display: inline-block;
}

.uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
}

.uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}

.uploaded-image {
  width: 118px;
  height: 118px;
  object-fit: cover;
  display: block;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #8c939d;
}

.placeholder-text {
  font-size: 12px;
}

.upload-tips {
  margin-top: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>