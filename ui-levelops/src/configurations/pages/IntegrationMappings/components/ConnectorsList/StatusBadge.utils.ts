import { Color } from "@harness/design-system";
import { IconName, IconProps } from "@harness/icons";

export const badgeIcon: Record<string, IconName> = {
  healthy: "heart",
  failed: "danger-icon",
  warning: "pause",
  unknown: "question"
};

export const color: Record<string, Color> = {
  healthy: Color.GREEN_800,
  failed: Color.RED_900,
  warning: Color.ORANGE_600,
  unknown: Color.GREY_700
};

export const backgroundColor: Record<string, Color> = {
  healthy: Color.GREEN_50,
  failed: Color.RED_50,
  warning: Color.YELLOW_100,
  unknown: Color.GREY_200
};

export const iconProps: Record<string, Partial<IconProps>> = {
  healthy: { color: Color.GREEN_700 },
  warning: { color: Color.ORANGE_600 },
  unknown: { color: Color.GREY_500 }
};
