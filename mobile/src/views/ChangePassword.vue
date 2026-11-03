<template>
  <div class="page-container">
    <PageHeader title="修改密码" />

    <p class="page-intro">为保障学籍安全，请定期更换密码</p>
    <van-form @submit="onSubmit" class="mt-8">
      <van-cell-group inset>
        <van-field
          v-model="form.oldPassword"
          type="password"
          label="原密码"
          placeholder="请输入原密码"
          :rules="[{ required: true, message: '请输入原密码' }]"
        />
        <van-field
          v-model="form.newPassword"
          type="password"
          label="新密码"
          placeholder="8-32位，含字母和数字"
          :rules="[
            { required: true, message: '请输入新密码' },
            { validator: validatePwd, message: '密码需8-32位，含字母和数字' },
          ]"
        />
        <van-field
          v-model="form.confirmPassword"
          type="password"
          label="确认密码"
          placeholder="请再次输入新密码"
          :rules="[
            { required: true, message: '请确认新密码' },
            { validator: validateConfirm, message: '两次密码不一致' },
          ]"
        />
      </van-cell-group>
      <div class="submit-wrap">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          确认修改
        </van-button>
      </div>
    </van-form>

    <p v-if="userStore.mustChangePwd" class="hint-text">
      首次登录需要修改密码后才能继续使用
    </p>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Form as VanForm, Field as VanField, CellGroup as VanCellGroup,
  Button as VanButton, showToast,
} from 'vant'
import PageHeader from '@/components/PageHeader.vue'
import { auth } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

function validatePwd(val: string) {
  return /^(?=.*[A-Za-z])(?=.*[0-9]).{8,32}$/.test(val)
}

function validateConfirm(val: string) {
  return (val === form.newPassword)
}

async function onSubmit() {
  loading.value = true
  try {
    await auth.changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    // 后端修改密码后会作废旧 token，需重新登录
    userStore.clearUser()
    showToast('密码修改成功，请重新登录')
    setTimeout(() => {
      router.replace('/login')
    }, 500)
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-intro {
  font-size: 12px;
  color: var(--text-faint);
  text-align: center;
  padding: 14px 16px 0;
  letter-spacing: 0.08em;
}
.submit-wrap {
  padding: 24px 16px;
}
.hint-text {
  text-align: center;
  font-size: 13px;
  color: var(--danger);
  padding: 0 16px;
}
</style>
