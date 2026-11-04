import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../store/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    perms?: string[]
    public?: boolean
    icon?: string
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: () => import('../layout/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台', perms: ['dashboard:view'] } },
      { path: 'project/list', component: () => import('../views/project/ProjectList.vue'), meta: { title: '培训班管理', perms: ['project:list'] } },
      { path: 'project/:id', component: () => import('../views/project/ProjectDetail.vue'), meta: { title: '培训班详情', perms: ['project:list'] } },
      { path: 'course/list', component: () => import('../views/course/CourseList.vue'), meta: { title: '课程管理', perms: ['course:list'] } },
      { path: 'course/edit/new', component: () => import('../views/course/CourseEdit.vue'), meta: { title: '新建课程', perms: ['course:add'] } },
      { path: 'course/edit/:id', component: () => import('../views/course/CourseEdit.vue'), meta: { title: '课程编辑', perms: ['course:edit'] } },
      { path: 'study/monitor', component: () => import('../views/study/Monitor.vue'), meta: { title: '学习监控', perms: ['study:monitor'] } },
      { path: 'verify/record', component: () => import('../views/verify/Records.vue'), meta: { title: '身份核验记录', perms: ['verify:list'] } },
      { path: 'verify/abnormal', component: () => import('../views/verify/Abnormal.vue'), meta: { title: '核验异常清单', perms: ['verify:abnormal'] } },
      { path: 'verify/gear', component: () => import('../views/verify/Gears.vue'), meta: { title: '核验档位模板', perms: ['verify:gear'] } },
      { path: 'credit/list', component: () => import('../views/credit/Ledger.vue'), meta: { title: '学分台账', perms: ['credit:list'] } },
      { path: 'credit/apply', component: () => import('../views/credit/Apply.vue'), meta: { title: '学分补录', perms: ['credit:apply'] } },
      { path: 'credit/audit', component: () => import('../views/credit/Audit.vue'), meta: { title: '补录审批', perms: ['credit:audit'] } },
      { path: 'graduate/apply', component: () => import('../views/graduate/Applies.vue'), meta: { title: '毕业申请', perms: ['graduate:list'] } },
      { path: 'graduate/file', component: () => import('../views/graduate/Files.vue'), meta: { title: '毕业档案', perms: ['graduate:file', 'graduate:list'] } },
      { path: 'report/org', component: () => import('../views/report/Index.vue'), meta: { title: '单位汇总', perms: ['report:org'], icon: 'org' } },
      { path: 'report/credit', component: () => import('../views/report/Index.vue'), meta: { title: '学分统计', perms: ['report:credit'], icon: 'credit' } },
      { path: 'report/course', component: () => import('../views/report/Index.vue'), meta: { title: '课程学习', perms: ['report:course'], icon: 'course' } },
      { path: 'report/warning', component: () => import('../views/report/Index.vue'), meta: { title: '学习预警', perms: ['report:warning'], icon: 'warning' } },
      { path: 'system/org', component: () => import('../views/system/Org.vue'), meta: { title: '组织架构', perms: ['system:org'] } },
      { path: 'system/user', component: () => import('../views/system/User.vue'), meta: { title: '账号管理', perms: ['system:user'] } },
      { path: 'system/role', component: () => import('../views/system/Role.vue'), meta: { title: '角色管理', perms: ['system:role'] } },
      { path: 'system/menu', component: () => import('../views/system/Menu.vue'), meta: { title: '菜单管理', perms: ['system:menu'] } },
      { path: 'system/config', component: () => import('../views/system/Config.vue'), meta: { title: '平台设置', perms: ['system:config'] } },
      { path: 'system/dict', component: () => import('../views/system/Dict.vue'), meta: { title: '数据字典', perms: ['system:dict'] } },
      { path: 'system/notice', component: () => import('../views/system/Notice.vue'), meta: { title: '消息模板', perms: ['system:notice'] } },
      { path: 'system/log', component: () => import('../views/system/OperLog.vue'), meta: { title: '操作日志', perms: ['system:log'] } },
      { path: 'system/loginlog', component: () => import('../views/system/LoginLog.vue'), meta: { title: '登录日志', perms: ['system:loginlog'] } },
      { path: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },
  { path: '/403', component: () => import('../views/Forbidden.vue'), meta: { title: '无权限', public: true } },
  { path: '/:pathMatch(.*)*', component: () => import('../views/NotFound.vue'), meta: { public: true, title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (to.path === '/login' && auth.isLogin) return '/dashboard'
    return true
  }
  if (!auth.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!auth.loaded) {
    try {
      await auth.refresh()
    } catch {
      auth.clear()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  if (auth.mustChangePwd && to.path !== '/profile') {
    return '/profile'
  }
  const required = to.meta.perms || []
  if (required.length && !required.some((perm) => auth.hasPerm(perm))) {
    return '/403'
  }
  return true
})

router.afterEach((to) => {
  const appTitle = import.meta.env.VITE_APP_TITLE || '继续教育学分管理平台'
  document.title = `${to.meta.title || '继续教育'} · ${appTitle}`
})

export default router
