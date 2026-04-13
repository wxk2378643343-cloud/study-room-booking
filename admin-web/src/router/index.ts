import { createRouter, createWebHistory } from "vue-router";
import AdminView from "../views/admin/AdminView.vue";
import LoginView from "../views/auth/LoginView.vue";
import ProfileEditView from "../views/user/ProfileEditView.vue";
import { useAuthStore } from "../stores/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/admin" },
    { path: "/login", name: "login", component: LoginView, meta: { guestOnly: true, plain: true } },
    { path: "/admin", name: "admin", component: AdminView, meta: { requiresAuth: true } },
    { path: "/profile", name: "profile", component: ProfileEditView, meta: { requiresAuth: true } }
  ]
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  const requiresAuth = Boolean(to.meta.requiresAuth);
  const guestOnly = Boolean(to.meta.guestOnly);

  if (requiresAuth && !auth.isLogin) {
    return "/login";
  }
  if (guestOnly && auth.isLogin) {
    return auth.isAdmin ? "/admin" : "/login";
  }
  if (requiresAuth && !auth.isAdmin) {
    return "/login";
  }
  return true;
});

export default router;
