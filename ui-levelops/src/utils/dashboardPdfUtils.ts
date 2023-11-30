import FileSaver from "file-saver";
import { PDFDocument, PDFImage, PDFPage, StandardFonts, rgb } from "pdf-lib";
import { divideArrayIntoChunks } from "./arrayUtils";
import { RestWidget } from "../classes/RestDashboards";
import { WidgetType } from "dashboard/helpers/helper";

const paddingLeft = 35;
const paddingRight = 35;
const paddingTop = 35;
const paddingBottom = 65;
const shiftContentToRight = paddingLeft;
const shiftContentToTop = paddingBottom;

export const generateReport = async (dashboard: any, dashboardWidgets: RestWidget[], widgetSVG: any) => {
  const title = dashboard.name;

  const _widgets = [
    ...dashboardWidgets.filter(
      (widget: { widget_type: string; hidden: boolean }) =>
        ["configurewidget", "compositegraph", "graph"].includes(widget.widget_type) && !widget.hidden
    )
  ];

  const _statsWidgets = [
    ...dashboardWidgets.filter(
      (widget: { widget_type: string; hidden: boolean }) => widget.widget_type.includes("stats") && !widget.hidden
    )
  ];

  const _notes = [
    ...dashboardWidgets.filter(
      (widget: { widget_type: string; hidden: boolean }) =>
        [WidgetType.STATS_NOTES, WidgetType.GRAPH_NOTES].includes(widget.widget_type as any) && !widget.hidden
    )
  ];

  const pdfDoc = await PDFDocument.create();

  const timesRomanFont = await pdfDoc.embedFont(StandardFonts.TimesRoman);

  const totalPages = () => widgets.length + _widgets.length + _notes.length;

  const addNewPage = (source: PDFDocument) => {
    const page = source.addPage();
    const width = page.getWidth();
    const height = page.getHeight();
    // TO change the orientation
    page.setHeight(width);
    page.setWidth(height);
    page.translateContent(shiftContentToRight, shiftContentToTop); // shift content to right and top.
    page.drawText(title, {
      x: (height - timesRomanFont.widthOfTextAtSize(title, 10)) / 2 - shiftContentToRight,
      y: width - 90,
      size: 10,
      font: timesRomanFont
    });
    const pageInfo = `${source.getPageCount()} / ${totalPages()}`;
    page.drawText(pageInfo, {
      x: (height - timesRomanFont.widthOfTextAtSize(pageInfo, 12)) / 2 - shiftContentToRight,
      y: 0 - 40,
      font: timesRomanFont,
      size: 12,
      color: rgb(0.6, 0.6, 0.6)
    });
    return page;
  };

  const widgets = divideArrayIntoChunks(_statsWidgets, 6);
  for (let j = 0; j < widgets.length; j++) {
    const statsWidget = widgets[j];
    const page = addNewPage(pdfDoc);
    for (let i = 0; i < statsWidget.length; i++) {
      const widget = statsWidget[i];
      await addStatsWidgetsToPage(page, await pdfDoc.embedPng(widgetSVG[widget.id]), i);
    }
  }

  for (let i = 0; i < _widgets.length; i++) {
    const page = addNewPage(pdfDoc);
    const widget = _widgets[i];
    await addFullPageImage(page, await pdfDoc.embedPng(widgetSVG[widget.id]));
  }

  for (let i = 0; i < _notes.length; i++) {
    const page = addNewPage(pdfDoc);
    const widget = _notes[i];
    await addFullPageImage(page, await pdfDoc.embedPng(widgetSVG[widget.id]));
  }

  const pdfBytes = await pdfDoc.save();
  const blob = new Blob([pdfBytes], { type: "application/pdf" });
  const blobUrl = URL.createObjectURL(blob);

  // @ts-ignore
  FileSaver.saveAs(blobUrl, `${dashboard.name}.pdf`, { type: "application/pdf" });

  return blob;
};

const getPageSize = (page: PDFPage) => ({
  width: page.getWidth() - (paddingLeft + paddingRight),
  height: page.getHeight() - (paddingTop + paddingBottom)
});

const addStatsWidgetsToPage = async (page: PDFPage, pdfImage: PDFImage, index = 0) => {
  const { width, height } = getPageSize(page);
  const paddingX = 50;
  const space = 10;
  let x = 0;
  if (index % 2 !== 0) {
    x = width / 2 + space;
  }
  let y = height * (2 / 3);
  if (index === 2 || index === 3) {
    y = height * (1 / 3);
  }
  if (index === 4 || index === 5) {
    y = 0;
  }
  page.drawImage(pdfImage, {
    x: x + paddingX,
    y,
    width: width / 2 - space - paddingX,
    height: height / 3 - space
  });
};

const addFullPageImage = async (page: PDFPage, pdfImage: PDFImage) => {
  const { width, height } = getPageSize(page);
  let imageHeight = pdfImage.height;
  let imageWidth = pdfImage.width;
  let StretchX = 0;
  let StretchY = 0;
  if (imageHeight > height || imageWidth > width) {
    const scale = Math.min(height / imageHeight, width / imageWidth);
    const scaledImage = pdfImage.scale(scale);
    imageHeight = scaledImage.height;
    imageWidth = scaledImage.width;
    if (width - imageWidth > 100) {
      StretchX = 100;
    }
    if (height - imageHeight > 100) {
      StretchY = 100;
    }
  }
  page.drawImage(pdfImage, {
    x: (width - imageWidth - StretchX) / 2,
    y: (height - imageHeight - StretchY) / 2,
    width: imageWidth + StretchX / 2,
    height: imageHeight + StretchY / 2
  });
};
