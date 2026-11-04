import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    /** 任一命中即放行，与后端 @PreAuthorize 使用同一套权限标识 */
    perms?: string[]
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/views/layout/TabLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/Home.vue') },
      { path: 'projects', name: 'Projects', component: () => import('@/views/Projects.vue') },
      { path: 'credits', name: 'Credits', component: () => import('@/views/Credits.vue') },
      {
        path: 'approvals',
        name: 'Approvals',
        component: () => import('@/views/Approvals.vue'),
        meta: { perms: ['credit:audit', 'graduate:list', 'verify:abnormal'] },
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/Monitor.vue'),
        meta: { perms: ['study:monitor'] },
      },
      { path: 'notifications', name: 'Notifications', component: () => import('@/views/Notifications.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue') },
    ],
  },
  {
    path: '/projects/:id',
    name: 'ProjectDetail',
    component: () => import('@/views/ProjectDetail.vue'),
  },
  {
    path: '/course/:id',
    name: 'CourseDetail',
    component: () => import('@/views/CourseDetail.vue'),
  },
  {
    path: '/study/:courseId',
    name: 'Study',
    component: () => import('@/views/Study.vue'),
  },
  {
    path: '/exam/:courseId',
    name: 'ExamPage',
    component: () => import('@/views/Exam.vue'),
  },
  {
    path: '/graduate',
    name: 'Graduate',
    component: () => import('@/views/Graduate.vue'),
  },
  {
    path: '/verify',
    name: 'VerifyRecords',
    component: () => import('@/views/VerifyRecords.vue'),
  },
  {
    path: '/study-record/:id',
    name: 'StudyRecordDetail',
    component: () => import('@/views/StudyRecordDetail.vue'),
    meta: { perms: ['study:monitor'] },
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('@/views/ChangePassword.vue'),
  },
  // 教务能力已拆到审批中心与学习监控，旧入口保持可达
  { path: '/teacher', redirect: '/approvals' },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, _from, next) => {
  if (to.meta.public) {
    return next()
  }
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) {
    return next('/login')
  }
  if (userStore.mustChangePwd && to.name !== 'ChangePassword') {
    return next('/change-password')
  }
  const required = to.meta.perms
  if (required?.length && !required.some((p) => userStore.hasPerm(p))) {
    showToast('没有该功能的访问权限')
    return next('/')
  }
  next()
})

export default router
