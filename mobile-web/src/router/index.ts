import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import LoginView from "../views/auth/LoginView.vue";
import RoomsView from "../views/room/RoomsView.vue";
import RoomDetailView from "../views/room/RoomDetailView.vue";
import BookingView from "../views/booking/BookingView.vue";
import UserCenterView from "../views/user/UserCenterView.vue";
import { useAuthStore } from "../stores/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "home", component: HomeView },
    { path: "/login", name: "login", component: LoginView },
    { path: "/rooms", name: "rooms", component: RoomsView },
    { path: "/rooms/:roomId", name: "room-detail", component: RoomDetailView },
    { path: "/booking", name: "booking", component: BookingView },
    { path: "/user", name: "user", component: UserCenterView }
  ]
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.path === "/login" || to.path === "/") {
    return true;
  }
  if (!auth.isLogin) {
    return "/login";
  }
  return true;
});

export default router;
