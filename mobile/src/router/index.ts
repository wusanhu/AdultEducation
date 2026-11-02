import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

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
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/Notifications.vue'),
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('@/views/ChangePassword.vue'),
  },
  {
    path: '/verify',
    name: 'VerifyRecords',
    component: () => import('@/views/VerifyRecords.vue'),
  },
  {
    path: '/teacher',
    name: 'Teacher',
    component: () => import('@/views/Teacher.vue'),
  },
]

const router = createRouter({
  history: createWebHashHistory(),
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
  if (to.name === 'Teacher' && !userStore.isTeacher) {
    return next('/')
  }
  next()
})

export default router
