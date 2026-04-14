import axios from "axios";

export const request = axios.create({
  baseURL: "/api/v1",
  timeout: 10000
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      if (window.location.pathname !== "/login") {
        const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`;
        window.location.replace(`/login?redirect=${encodeURIComponent(redirect)}`);
      }
    }
    return Promise.reject(error);
  }
);

export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export async function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const { data } = await request.get<ApiResponse<T>>(url, { params });
  return data.data;
}

export async function apiPost<T>(url: string, body?: unknown): Promise<T> {
  const { data } = await request.post<ApiResponse<T>>(url, body);
  return data.data;
}

export async function apiPut<T>(url: string, body?: unknown): Promise<T> {
  const { data } = await request.put<ApiResponse<T>>(url, body);
  return data.data;
}

export async function apiUpload<T>(url: string, formData: FormData): Promise<T> {
  const { data } = await request.post<ApiResponse<T>>(url, formData);
  return data.data;
}
