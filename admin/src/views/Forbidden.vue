<template>
  <div class="page-card" style="max-width:520px;margin:80px auto;text-align:center">
    <a-result status="403" title="没有访问权限" sub-title="当前账号无权访问该功能，请联系管理员调整角色权限。">
      <template #extra>
        <a-button type="primary" @click="$router.replace('/dashboard')">返回工作台</a-button>
      </template>
    </a-result>
  </div>
</template>
