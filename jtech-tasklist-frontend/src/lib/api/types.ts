// Auth
export interface LoginRequest {
  email: string;
  password: string;
}
export interface TokenResponse {
  token: string;
}

// Users
export interface UserCreateRequest {
  name: string;
  email: string;
  password: string;
}
export interface UserResponse {
  id: number;
  name: string;
  email: string;
  createdAt: string;
  updatedAt: string;
}

// Folders
export interface FolderCreateRequest {
  name: string;
  isPublic?: boolean;
}
export interface FolderResponse {
  id: number;
  name: string;
  isPublic: boolean;
  key: string | null;
  ownerId: number;
}
export interface FolderJoinRequest {
  key: string;
}
export interface FolderMemberResponse {
  userId: number;
  name: string;
  email: string;
  joinedAt: string;
}

// Tags
export interface TagCreateRequest {
  name: string;
}
export interface TagResponse {
  id: number;
  name: string;
  color: string;
  folderId: number;
}

// Tasks
export interface TaskCreateRequest {
  title: string;
  description?: string | null;
  dueDate?: string | null;
  parentTaskId?: number | null;
  tagIds?: number[] | null;
}
export interface TaskUpdateRequest {
  title?: string;
  description?: string | null;
  dueDate?: string | null;
  completed?: boolean | null;
  tagIds?: number[] | null;
}
export interface SubtaskResponse {
  id: number;
  title: string;
  completed: boolean;
}
export interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  dueDate: string | null;
  completed: boolean;
  folderId: number;
  parentTaskId: number | null;
  tagIds: number[];
  subtasks: SubtaskResponse[];
}
