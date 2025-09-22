import http from '@/lib/api/client';
import type { TaskCreateRequest, TaskResponse, TaskUpdateRequest } from '@/lib/api/types';

export const tasksService = {
  create(folderId: number, payload: TaskCreateRequest) {
    return http.post<TaskResponse, TaskCreateRequest>(`/api/folders/${folderId}/tasks`, payload);
  },
  list(folderId: number) {
    return http.get<TaskResponse[]>(`/api/folders/${folderId}/tasks`);
  },
  getById(folderId: number, taskId: number) {
    return http.get<TaskResponse>(`/api/folders/${folderId}/tasks/${taskId}`);
  },
  update(folderId: number, taskId: number, payload: TaskUpdateRequest) {
    return http.put<TaskResponse, TaskUpdateRequest>(`/api/folders/${folderId}/tasks/${taskId}`, payload);
  },
  setCompleted(folderId: number, taskId: number, completed: boolean) {
    return http.patch<TaskResponse, undefined>(`/api/folders/${folderId}/tasks/${taskId}/completed`, undefined, true, { completed });
  },
  delete(folderId: number, taskId: number) {
    return http.del<void>(`/api/folders/${folderId}/tasks/${taskId}`);
  },
};

export default tasksService;
