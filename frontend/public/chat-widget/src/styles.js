/* =========================================================
   AI 客服聊天组件样式（注入 Shadow DOM）
   ========================================================= */

const styles = `
  :host {
    --widget-primary: #1677ff;
    --widget-bg: #ffffff;
    --widget-text: #1f1f1f;
    --widget-text-secondary: #8c8c8c;
    --widget-border: #e8e8e8;
    --widget-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
    --widget-radius: 12px;
    --widget-max-width: 380px;
    --widget-max-height: 600px;
    --widget-font-size: 14px;
    --widget-line-height: 1.6;
    --widget-user-bg: #1677ff;
    --widget-user-color: #ffffff;
    --widget-assistant-bg: #f5f5f5;
    --widget-assistant-color: #1f1f1f;
    all: initial;
    display: block;
  }

  * {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
  }

  /* ── 触发器按钮 ── */
  #trigger {
    position: fixed;
    bottom: 24px;
    right: 24px;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: var(--widget-primary);
    color: #fff;
    border: none;
    cursor: pointer;
    box-shadow: 0 4px 16px rgba(22, 119, 255, 0.35);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.2s, box-shadow 0.2s;
    z-index: 2147483647;
  }

  #trigger:hover {
    transform: scale(1.08);
    box-shadow: 0 6px 24px rgba(22, 119, 255, 0.45);
  }

  #trigger svg {
    width: 28px;
    height: 28px;
    fill: currentColor;
  }

  /* ── 聊天面板 ── */
  #panel {
    position: fixed;
    bottom: 90px;
    right: 24px;
    width: var(--widget-max-width);
    max-height: var(--widget-max-height);
    background: var(--widget-bg);
    border-radius: var(--widget-radius);
    box-shadow: var(--widget-shadow);
    display: none;
    flex-direction: column;
    overflow: hidden;
    z-index: 2147483646;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    font-size: var(--widget-font-size);
    line-height: var(--widget-line-height);
    color: var(--widget-text);
    animation: slideUp 0.25s ease-out;
  }

  #panel.open {
    display: flex;
  }

  @keyframes slideUp {
    from { opacity: 0; transform: translateY(16px); }
    to   { opacity: 1; transform: translateY(0); }
  }

  /* ── 标题栏 ── */
  #header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 18px;
    background: var(--widget-primary);
    color: #fff;
    flex-shrink: 0;
  }

  #header-title {
    font-weight: 600;
    font-size: 15px;
  }

  #header-close {
    background: none;
    border: none;
    color: #fff;
    cursor: pointer;
    padding: 4px;
    border-radius: 4px;
    opacity: 0.8;
    transition: opacity 0.15s;
    line-height: 0;
  }

  #header-close:hover { opacity: 1; }

  /* ── 消息列表 ── */
  #messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px 18px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-height: 0;
    scroll-behavior: smooth;
  }

  #messages::-webkit-scrollbar {
    width: 4px;
  }
  #messages::-webkit-scrollbar-thumb {
    background: #d9d9d9;
    border-radius: 2px;
  }

  /* ── 消息气泡 ── */
  .msg {
    max-width: 85%;
    padding: 10px 14px;
    border-radius: 10px;
    word-wrap: break-word;
    white-space: pre-wrap;
    animation: fadeIn 0.2s ease-out;
  }

  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(4px); }
    to   { opacity: 1; transform: translateY(0); }
  }

  .msg.user {
    align-self: flex-end;
    background: var(--widget-user-bg);
    color: var(--widget-user-color);
    border-bottom-right-radius: 4px;
  }

  .msg.assistant {
    align-self: flex-start;
    background: var(--widget-assistant-bg);
    color: var(--widget-assistant-color);
    border-bottom-left-radius: 4px;
  }

  .msg.assistant p {
    margin: 0 0 6px;
  }
  .msg.assistant p:last-child {
    margin-bottom: 0;
  }
  .msg.assistant ul, .msg.assistant ol {
    padding-left: 20px;
    margin: 4px 0;
  }
  .msg.assistant code {
    background: rgba(0,0,0,0.06);
    padding: 1px 4px;
    border-radius: 3px;
    font-size: 0.9em;
    font-family: 'SFMono-Regular', Consolas, monospace;
  }
  .msg.assistant strong {
    font-weight: 600;
  }

  .msg.sources {
    align-self: flex-start;
    background: #fffbe6;
    color: #8c6e00;
    font-size: 12px;
    padding: 8px 12px;
    border-radius: 8px;
    max-width: 100%;
  }

  .msg.sources strong {
    display: block;
    margin-bottom: 4px;
    font-size: 12px;
  }

  /* ── 加载动画 ── */
  .typing {
    align-self: flex-start;
    display: flex;
    gap: 4px;
    padding: 12px 16px;
    background: var(--widget-assistant-bg);
    border-radius: 10px;
    border-bottom-left-radius: 4px;
  }

  .typing span {
    width: 7px;
    height: 7px;
    background: #bfbfbf;
    border-radius: 50%;
    animation: bounce 1.4s ease-in-out infinite;
  }

  .typing span:nth-child(2) { animation-delay: 0.2s; }
  .typing span:nth-child(3) { animation-delay: 0.4s; }

  @keyframes bounce {
    0%, 60%, 100% { transform: translateY(0); }
    30% { transform: translateY(-6px); }
  }

  /* ── 输入区 ── */
  #input-area {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    padding: 12px 18px;
    border-top: 1px solid var(--widget-border);
    flex-shrink: 0;
    background: var(--widget-bg);
  }

  #input {
    flex: 1;
    border: 1px solid var(--widget-border);
    border-radius: 8px;
    padding: 8px 12px;
    font-size: var(--widget-font-size);
    font-family: inherit;
    line-height: 1.4;
    resize: none;
    outline: none;
    transition: border-color 0.2s;
    max-height: 120px;
    min-height: 36px;
    color: var(--widget-text);
    background: var(--widget-bg);
  }

  #input:focus {
    border-color: var(--widget-primary);
  }

  #input::placeholder {
    color: var(--widget-text-secondary);
  }

  #send-btn {
    flex-shrink: 0;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--widget-primary);
    color: #fff;
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: opacity 0.15s;
    line-height: 0;
  }

  #send-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  #send-btn svg {
    width: 16px;
    height: 16px;
    fill: currentColor;
  }

  /* ── 错误提示 ── */
  .error-msg {
    align-self: center;
    color: #ff4d4f;
    font-size: 12px;
    padding: 6px 12px;
    background: #fff2f0;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.15s;
  }

  .error-msg:hover {
    background: #ffd8d3;
  }

  /* ── 响应式 ── */
  @media (max-width: 480px) {
    #panel {
      right: 8px;
      bottom: 80px;
      width: calc(100vw - 16px);
      max-height: calc(100vh - 100px);
      border-radius: 10px;
    }
    #trigger {
      bottom: 16px;
      right: 16px;
      width: 48px;
      height: 48px;
    }
  }
`;

export default styles;
