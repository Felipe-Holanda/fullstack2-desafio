import http from '@/lib/api/client';
import type { TagCreateRequest, TagResponse } from '@/lib/api/types';

export const tagsService = {
  create(folderId: number, payload: TagCreateRequest) {
    return http.post<TagResponse, TagCreateRequest>(`/api/folders/${folderId}/tags`, payload);
  },
  list(folderId: number) {
    return http.get<TagResponse[]>(`/api/folders/${folderId}/tags`);
  },
  delete(folderId: number, tagId: number) {
    return http.del<void>(`/api/folders/${folderId}/tags/${tagId}`);
  },
};

export default tagsService;
