// scripts/update-readme-tasks.js
const fs = require('fs');
const path = require('path');

// 1. 실제 파일 위치 (.taskmaster/tasks/tasks.json)
const tasksPath = path.join(__dirname, '..', '.taskmaster', 'tasks', 'tasks.json');
const readmePath = path.join(__dirname, '..', 'README.md');

// 2. tasks.json 읽기
const raw = JSON.parse(fs.readFileSync(tasksPath, 'utf8'));

// 3. 구조에 맞게 tasks 배열 꺼내기
let tasks = [];

if (raw && raw.master && Array.isArray(raw.master.tasks)) {
  tasks = raw.master.tasks;
} else {
  console.error('❌ 예상한 구조(raw.master.tasks)가 아닙니다. raw =', raw);
  process.exit(1);
}

// 4. JSON → Markdown 테이블 변환
function toMarkdownTable(tasks) {
  const headers = ['ID', 'Title', 'Status', 'Priority', 'Subtasks'];
  const lines = [];

  lines.push(`| ${headers.join(' | ')} |`);
  lines.push(`| ${headers.map(() => '---').join(' | ')} |`);

  for (const t of tasks) {
    const id = t.id ?? '';
    const title = t.title ?? '';
    const status = t.status ?? '';          // pending, done 등
    const priority = t.priority ?? '';
    const subtaskCount = Array.isArray(t.subtasks) ? t.subtasks.length : 0;

    lines.push(
      `| ${id} | ${title} | ${status} | ${priority} | ${subtaskCount} |`
    );
  }

  return lines.join('\n');
}

const table = toMarkdownTable(tasks);

// 5. README.md 읽어서 마커 구간 교체
let readme = fs.readFileSync(readmePath, 'utf8');

const markerStart = '<!-- TASKS-TABLE:START -->';
const markerEnd = '<!-- TASKS-TABLE:END -->';

// README에 해당 마커가 없으면 안내하고 종료
if (!readme.includes(markerStart) || !readme.includes(markerEnd)) {
  console.error(
    '❌ README.md 안에 마커가 없습니다.\n' +
    '아래와 같이 README에 구간을 먼저 만들어 주세요:\n\n' +
    `${markerStart}\n\n(자동 생성 영역)\n\n${markerEnd}\n`
  );
  process.exit(1);
}

const newBlock = `${markerStart}\n\n${table}\n\n${markerEnd}`;

const pattern = new RegExp(
  `${markerStart}[\\s\\S]*?${markerEnd}`
);

readme = readme.replace(pattern, newBlock);

// 6. README 저장
fs.writeFileSync(readmePath, readme);
console.log('✅ README tasks section updated.');

