import { cloneDeep } from "lodash";

export const sortedCategories = (categories: any[]) => {
  return categories.sort((a, b) => a.index - b.index);
};

export const categoryMoveUp = (categories: any[], categoryId: string) => {
  const categoriesToUpdate = cloneDeep(categories);
  const index = categoriesToUpdate.findIndex(category => category.id === categoryId);
  if (index - 1 < 0) return categoriesToUpdate;
  const lowerCategory = categoriesToUpdate[index];
  const upperCategory = categoriesToUpdate[index - 1];
  const tempPlaceholder = upperCategory?.index;
  upperCategory.index = lowerCategory.index;
  lowerCategory.index = tempPlaceholder;
  return sortedCategories(categoriesToUpdate);
};

export const categoryMoveDown = (categories: any[], categoryId: string) => {
  const categoriesToUpdate = cloneDeep(categories);
  const index = categoriesToUpdate.findIndex(category => category.id === categoryId);
  if (index + 1 >= categoriesToUpdate.length) return categoriesToUpdate;
  const lowerCategory = categoriesToUpdate[index + 1];
  const upperCategory = categoriesToUpdate[index];
  const tempPlaceholder = upperCategory?.index;
  upperCategory.index = lowerCategory.index;
  lowerCategory.index = tempPlaceholder;
  return sortedCategories(categoriesToUpdate);
};

export const categoryRankToTop = (categories: any[], categoryId: string) => {
  const categoriesToUpdate = cloneDeep(categories);
  const index = categoriesToUpdate.findIndex(category => category.id === categoryId);
  const lowerCategory = categoriesToUpdate[index];
  lowerCategory.index = 1;
  for (let i = 0; i < index; i++) {
    const curCategory = categoriesToUpdate[i];
    curCategory.index = curCategory.index + 1;
  }
  return sortedCategories(categoriesToUpdate);
};

export const categoryRankToBottom = (categories: any[], categoryId: string) => {
  const categoriesToUpdate = cloneDeep(categories);
  const index = categoriesToUpdate.findIndex(category => category.id === categoryId);
  const upperCategory = categoriesToUpdate[index];
  upperCategory.index = categoriesToUpdate.length;
  for (let i = index + 1; i < categoriesToUpdate.length; i++) {
    const curCategory = categoriesToUpdate[i];
    curCategory.index = curCategory.index - 1;
  }
  return sortedCategories(categoriesToUpdate);
};
