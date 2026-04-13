import { defineStore } from "pinia";

type UserInfo = {
  userId: number;
  nickname: string;
  role: number;
};

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("token") || "",
    user: (localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user") as string) : null) as UserInfo | null
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === 9
  },
  actions: {
    setLogin(token: string, user: UserInfo) {
      this.token = token;
      this.user = user;
      localStorage.setItem("token", token);
      localStorage.setItem("user", JSON.stringify(user));
    },
    updateNickname(nickname: string) {
      if (!this.user) return;
      this.user = { ...this.user, nickname };
      localStorage.setItem("user", JSON.stringify(this.user));
    },
    logout() {
      this.token = "";
      this.user = null;
      localStorage.removeItem("token");
      localStorage.removeItem("user");
    }
  }
});
