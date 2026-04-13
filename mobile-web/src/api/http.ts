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
