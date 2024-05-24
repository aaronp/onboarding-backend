import './style.css'
import 'scalajs:main.mjs'
import mermaid from 'mermaid';

/** We call this from MermaidPage, sending it a new element which contains mermaid markdown  */
function renderMermaid(targetElm) {
  mermaid.initialize({ startOnLoad: false });
  mermaid.run({
    nodes: [targetElm],
    suppressErrors: false,
  }).then(() => {
    console.log('Rendered mermaid diagram');
  });
}

window.renderMermaid = renderMermaid;
export default { renderMermaid };
