import { Router, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { authMiddleware, AuthRequest } from '../middleware/auth';
import { createHash } from 'crypto';
import AdmZip from 'adm-zip';

const router = Router();
const prisma = new PrismaClient();
router.use(authMiddleware);

const EMPTY_DATA = {
  version: '1.0',
  exportedAt: 0,
  ledgers: [],
  members: [],
  expenses: [],
  expenseSplits: [],
  settlements: [],
  exchangeRates: [],
};

function sha256(s: string): string {
  return createHash('sha256').update(s, 'utf8').digest('hex');
}

/** 合并单表差异（主键为自增 id） */
function mergeTable(existing: any[], diff: any): any[] {
  const map = new Map<number, any>();
  for (const e of existing ?? []) if (e && e.id != null) map.set(e.id, e);
  for (const id of diff?.deletedIds ?? []) map.delete(id);
  for (const e of diff?.added ?? []) map.set(e.id, e);
  for (const e of diff?.updated ?? []) map.set(e.id, e);
  return Array.from(map.values());
}

/** 合并汇率表差异（主键为 currencyCode） */
function mergeRates(existing: any[], diff: any): any[] {
  const map = new Map<string, any>();
  for (const e of existing ?? []) if (e && e.currencyCode != null) map.set(e.currencyCode, e);
  for (const c of diff?.deletedCodes ?? []) map.delete(c);
  for (const e of diff?.added ?? []) map.set(e.currencyCode, e);
  for (const e of diff?.updated ?? []) map.set(e.currencyCode, e);
  return Array.from(map.values());
}

/** 从上传 zip 中读取 images/ 下的文件，key 去掉 images/ 前缀 */
function readImages(zip: AdmZip): Map<string, Buffer> {
  const map = new Map<string, Buffer>();
  for (const entry of zip.getEntries()) {
    if (entry.entryName.startsWith('images/') && !entry.isDirectory) {
      map.set(entry.entryName.replace(/^images\//, ''), entry.getData());
    }
  }
  return map;
}

async function saveImages(userId: number, images: Map<string, Buffer>): Promise<void> {
  for (const [key, buf] of images) {
    await prisma.userBackupImage.upsert({
      where: { userId_imageKey: { userId, imageKey: key } },
      create: { userId, imageKey: key, data: buf },
      update: { data: buf },
    });
  }
}

// 下载：组装完整 zip（data.json + manifest.json + images/*），内存打包后用 Content-Length 发送
router.get('/', async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const backup = await prisma.userBackup.findUnique({ where: { userId: req.userId! } });
    if (!backup) { res.status(404).json({ error: '暂无备份' }); return; }

    const images = await prisma.userBackupImage.findMany({ where: { userId: req.userId! } });
    const data = backup.data as any;
    const dataJson = JSON.stringify(data);
    const manifest = {
      version: '1.0',
      exportedAt: data?.exportedAt ?? Date.now(),
      checksum: sha256(dataJson),
      imageCount: images.length,
      revision: backup.revision,
    };

    const zip = new AdmZip();
    zip.addFile('data.json', Buffer.from(dataJson, 'utf8'));
    zip.addFile('manifest.json', Buffer.from(JSON.stringify(manifest), 'utf8'));
    for (const img of images) {
      zip.addFile(`images/${img.imageKey}`, img.data);
    }
    const buf = zip.toBuffer();

    res.setHeader('Content-Type', 'application/zip');
    res.setHeader('Content-Length', buf.length);
    res.setHeader('Content-Disposition', 'attachment; filename="backup.zip"');
    res.send(buf);
  } catch (err) {
    console.error('Download error:', err);
    if (!res.headersSent) res.status(500).json({ error: '服务器错误' });
  }
});

// 上传：全量（data.json）或增量（changes.json + 变化图片）
router.post('/', async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const body: Buffer | undefined = req.body;
    if (!body || body.length === 0) { res.status(400).json({ error: '空请求体' }); return; }

    const mode = (req.headers['x-backup-mode'] as string) || 'full';
    const baseRevision = parseInt((req.headers['x-base-revision'] as string) || '0', 10) || 0;
    const zip = new AdmZip(body);

    if (mode === 'full') {
      const data = JSON.parse(zip.readAsText('data.json'));
      const images = readImages(zip);
      const current = await prisma.userBackup.findUnique({ where: { userId: req.userId! } });
      const revision = (current?.revision ?? 0) + 1;

      await prisma.userBackup.upsert({
        where: { userId: req.userId! },
        create: { userId: req.userId!, data, revision },
        update: { data, revision },
      });
      await prisma.userBackupImage.deleteMany({ where: { userId: req.userId! } });
      await saveImages(req.userId!, images);

      res.json({ message: '已保存', revision });
      return;
    }

    // 增量：校验 baseRevision，合并差异
    const changes = JSON.parse(zip.readAsText('changes.json'));
    const current = await prisma.userBackup.findUnique({ where: { userId: req.userId! } });
    const currentRev = current?.revision ?? 0;
    if (baseRevision !== currentRev) {
      res.status(409).json({ error: '版本冲突' });
      return;
    }

    const data = (current?.data as any) ?? { ...EMPTY_DATA, exportedAt: Date.now() };
    data.ledgers = mergeTable(data.ledgers, changes.ledgers);
    data.members = mergeTable(data.members, changes.members);
    data.expenses = mergeTable(data.expenses, changes.expenses);
    data.expenseSplits = mergeTable(data.expenseSplits, changes.expenseSplits);
    data.settlements = mergeTable(data.settlements, changes.settlements);
    data.exchangeRates = mergeRates(data.exchangeRates, changes.exchangeRates);

    const images = readImages(zip);
    for (const key of changes.images?.deleteKeys ?? []) {
      await prisma.userBackupImage.deleteMany({ where: { userId: req.userId!, imageKey: key } });
    }
    const upsertKeys = new Set<string>(changes.images?.upsertKeys ?? []);
    for (const [key, buf] of images) {
      if (upsertKeys.has(key)) {
        await prisma.userBackupImage.upsert({
          where: { userId_imageKey: { userId: req.userId!, imageKey: key } },
          create: { userId: req.userId!, imageKey: key, data: buf },
          update: { data: buf },
        });
      }
    }

    const revision = currentRev + 1;
    await prisma.userBackup.update({ where: { userId: req.userId! }, data: { data, revision } });
    res.json({ message: '已保存', revision });
  } catch (err) {
    console.error('Upload error:', err);
    res.status(500).json({ error: '服务器错误' });
  }
});

export { router as backupRouter };
