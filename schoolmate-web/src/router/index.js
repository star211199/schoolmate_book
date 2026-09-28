import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/views/LayoutView.vue'),
    redirect: '/classes',
    children: [
      {
        path: 'classes',
        name: 'Classes',
        component: () => import('@/views/class/ClassListView.vue'),
        meta: { title: '班级广场', requiresAuth: true }
      },
      {
        path: 'classes/:id',
        name: 'ClassDetail',
        component: () => import('@/views/class/ClassDetailView.vue'),
        meta: { title: '班级主页', requiresAuth: true }
      },
      {
        path: 'users/:id',
        name: 'UserProfile',
        component: () => import('@/views/profile/UserProfileView.vue'),
        meta: { title: '同学主页', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'MyProfile',
        component: () => import('@/views/profile/MyProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/views/admin/AdminLayout.vue'),
        redirect: '/admin/dashboard',
        meta: { requiresAuth: true, requiresAdmin: true },
        children: [
          {
            path: 'dashboard',
            name: 'AdminDashboard',
            component: () => import('@/views/admin/DashboardView.vue'),
            meta: { title: '数据概览' }
          },
          {
            path: 'users',
            name: 'AdminUsers',
            component: () => import('@/views/admin/UserManageView.vue'),
            meta: { title: '用户管理' }
          },
          {
            path: 'audit',
            name: 'AdminAudit',
            component: () => import('@/views/admin/AuditManageView.vue'),
            meta: { title: '内容审核' }
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：登录校验 + 管理员权限校验
router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 大学同学录` : '大学同学录'

  const userStore = useUserStore()
  const needAuth = to.matched.some((r) => r.meta.requiresAuth)

  if (!needAuth) {
    return next()
  }
  if (!userStore.isLogin) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  // Token 存在但用户信息缺失时（刷新页面）重新拉取
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (e) {
      userStore.logout()
      return next({ path: '/login' })
    }
  }
  if (to.matched.some((r) => r.meta.requiresAdmin) && !userStore.isAdmin) {
    return next({ path: '/classes' })
  }
  return next()
})

export default router
