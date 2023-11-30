import { RestTicketCategorizationCategory } from "classes/RestTicketCategorizationScheme";
import { CustomLayout } from "dashboard/components/rearrange-grid/helper";

export const categoryRankColumnsConfig = [
  {
    title: "Rank",
    key: "index",
    dataIndex: "index",
    width: "10%"
  },
  {
    title: "Category Name",
    key: "name",
    dataIndex: "name",
    width: "25%",
    ellipsis: true
  },
  {
    title: "Description",
    key: "description",
    dataIndex: "description",
    width: "65%",
    ellipsis: true
  }
];

export const getCategoriesLayout = (categories: RestTicketCategorizationCategory[]) => {
  let row = 0;
  let col = 0;
  let _y = 0;
  let last_row = 0;
  const colCount = 3;
  const sortedData = categories.sort((a: any, b: any) => a.index - b.index);
  const customLayout: CustomLayout[] = sortedData.map((item: RestTicketCategorizationCategory, index: number) => {
    const widgetHeight = 5;
    if (index > 0) {
      col = col + 1;
    }

    if (col === colCount) {
      col = 0;
      row = row + 1;
    }

    if (last_row < row) {
      _y = _y + widgetHeight;
      last_row = row;
    }

    let w = 1;

    return {
      i: item.id,
      x: col,
      y: _y,
      w,
      h: widgetHeight,
      add: false,
      data: item,
      position: index
    } as any;
  });
  return customLayout;
};
