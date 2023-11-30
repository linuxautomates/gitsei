interface ScrollOptions {
  top: number;
  left: number;
  behavior?: "smooth";
}
export const scrollElementById = (element_id: string, scrollOptions: ScrollOptions) => {
  const node = document.getElementById(element_id);
  if (node) {
    node.scrollTo(scrollOptions);
  }
};
