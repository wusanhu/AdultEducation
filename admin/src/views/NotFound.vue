<template>
  <div class="page-card" style="max-width:520px;margin:80px auto">
    <a-result status="404" title="页面不存在" sub-title="请检查访问地址，或返回工作台继续操作。">
      <template #extra>
        <a-button type="primary" @click="$router.replace('/dashboard')">返回工作台</a-button>
      </template>
    </a-result>
  </div>
</template>
