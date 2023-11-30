import { cloneDeep, get, set } from "lodash";
import { Widget } from "model/entities/Widget";
import { colorPalletteShades } from "shared-resources/charts/chart-themes";

/**
 * It takes a widget, checks if it has a yAxis, and if it does, it checks if it has a display_color,
 * and if it doesn't, it assigns it a color from a list of colors
 * @param {Widget} widget - Widget - The widget object that is being transformed.
 * @returns A widget
 */

export function levelopsTableReportPrevQueryTransformer(widget: Widget): Widget {
  const { metadata } = widget;
  const yAxises = get(metadata, ["yAxis"], []);
  if (yAxises && yAxises.length) {
    const alreadyUsedColors: string[] = yAxises
      ?.filter((y: { display_color?: string }) => !!y.display_color)
      .map((y: { display_color: string }) => y.display_color.toLowerCase());
    const defaultDisplayColors = colorPalletteShades.filter(c => !alreadyUsedColors.includes(c?.toLowerCase()));
    let idx = 0;
    const newYAxises = yAxises.map((y: { display_color?: string }) => {
      if (!y.display_color) {
        y.display_color = defaultDisplayColors[idx];
        idx += 1;
      }
      return y;
    });
    set(metadata, ["yAxis"], newYAxises);
    set(widget, ["metadata"], metadata);
  }
  return widget;
}
