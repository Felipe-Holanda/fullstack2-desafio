import http from '@/lib/api/client';
import type {
  FolderCreateRequest,
  FolderJoinRequest,
  FolderMemberResponse,
  FolderResponse,
} from '@/lib/api/types';

export const foldersService = {
  // Create folder
  create(payload: FolderCreateRequest) {
    return http.post<FolderResponse, FolderCreateRequest>('/api/folders', payload);
  },

  // Update folder (e.g., name, isPublic)
  update(folderId: number, payload: Partial<FolderCreateRequest>) {
    return http.patch<FolderResponse, Partial<FolderCreateRequest>>(`/api/folders/${folderId}`, payload);
  },

  // List mine
  listMine() {
    return http.get<FolderResponse[]>('/api/folders');
  },

  // List participating
  listParticipating() {
    return http.get<FolderResponse[]>('/api/folders/participating');
  },

  // List all
  listAll() {
    return http.get<FolderResponse[]>('/api/folders/all');
  },

  // Join public via key
  join(payload: FolderJoinRequest) {
    return http.post<FolderResponse, FolderJoinRequest>('/api/folders/join', payload);
  },

  // Rotate key
  rotateKey(folderId: number) {
    return http.post<FolderResponse, undefined>(`/api/folders/${folderId}/rotate-key`);
  },

  // Delete folder
  delete(folderId: number) {
    return http.del<void>(`/api/folders/${folderId}`);
  },

  // Members
  listMembers(folderId: number) {
    return http.get<FolderMemberResponse[]>(`/api/folders/${folderId}/members`);
  },
  removeMember(folderId: number, userId: number) {
    return http.del<void>(`/api/folders/${folderId}/members/${userId}`);
  },
};

export default foldersService;
