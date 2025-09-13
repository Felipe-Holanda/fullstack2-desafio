import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import TasksSection from '@/components/dashboard/TasksSection.vue';

const baseProps = {
  selectedFolder: { id: 1, name: 'Pasta' },
  tasks: [
    { id: 10, title: 'Tarefa A', description: 'Desc', completed: false, tagIds: [], subtasks: [], createdAt: '', updatedAt: '' },
  ],
  tags: [],
  tasksLoading: false,
  tagMap: new Map(),
  creatingTask: false,
  newTask: { title: '', description: '' },
};

describe('TasksSection', () => {
  it('renders tasks and emits events', async () => {
    const wrapper = mount(TasksSection, { props: baseProps });
    expect(wrapper.text()).toContain('Tarefa A');
    await wrapper.find('button[title="Ver detalhes"]').trigger('click');
    expect(wrapper.emitted('open-task')).toBeTruthy();
  });

  it('emits add-task when clicking add', async () => {
    const wrapper = mount(TasksSection, { props: { ...baseProps, newTask: { title: 'Nova', description: '' } } });
    const addBtn = wrapper.find('button.add-task-btn');
    expect(addBtn.attributes('disabled')).toBeUndefined();
    await addBtn.trigger('click');
    expect(wrapper.emitted('add-task')).toBeTruthy();
  });
});
