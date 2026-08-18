import express from 'express';
import cors from 'cors';
import { authRouter } from './routes/auth';
import { adminRouter } from './routes/admin';
import { backupRouter } from './routes/backup';
import { ledgersRouter } from './routes/ledgers';
import { expensesRouter } from './routes/expenses';
import { settlementsRouter } from './routes/settlements';

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json({ limit: '50mb' }));

app.get('/api/health', (_req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

app.use('/api/auth', authRouter);
app.use('/api/admin', adminRouter);
// 备份路由接收二进制 zip 上传（全局 express.json 不处理 application/zip）
app.use('/api/backup', express.raw({ type: 'application/zip', limit: '200mb' }), backupRouter);
app.use('/api/ledgers', ledgersRouter);
app.use('/api/expenses', expensesRouter);
app.use('/api/settlements', settlementsRouter);

app.listen(PORT, () => {
  console.log(`AA Ledger API running on port ${PORT}`);
});
