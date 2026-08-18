import { Router, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { z } from 'zod';
import { authMiddleware, AuthRequest } from '../middleware/auth';
import crypto from 'crypto';

const router = Router();
const prisma = new PrismaClient();
router.use(authMiddleware);

function genCode(): string { return crypto.randomInt(100000, 999999).toString(); }

router.get('/', async (req: AuthRequest, res: Response): Promise<void> => {
  const memberships = await prisma.ledgerMember.findMany({
    where: { userId: req.userId },
    include: { ledger: { include: { members: { include: { user: { select: { id: true, nickname: true } } } }, expenses: { select: { totalAmountCny: true } } } } },
    orderBy: { ledger: { updatedAt: 'desc' } },
  });
  res.json({ ledgers: memberships.map(m => ({
    id: m.ledger.id, name: m.ledger.name, description: m.ledger.description,
    coverType: m.ledger.coverType, baseCurrency: m.ledger.baseCurrency,
    budgetAmount: m.ledger.budgetAmount, ownerId: m.ledger.ownerId,
    inviteCode: m.ledger.inviteCode, memberCount: m.ledger.members.length,
    members: m.ledger.members.map(mb => ({ userId: mb.user.id, nickname: mb.user.nickname })),
    totalExpense: m.ledger.expenses.reduce((sum: number, e: any) => sum + e.totalAmountCny, 0),
    expenseCount: m.ledger.expenses.length, createdAt: m.ledger.createdAt, updatedAt: m.ledger.updatedAt,
  })) });
});

router.post('/', async (req: AuthRequest, res: Response): Promise<void> => {
  const p = z.object({ name: z.string().min(1), description: z.string().optional(), coverType: z.string().optional(), baseCurrency: z.string().optional(), budgetAmount: z.number().optional() }).safeParse(req.body);
  if (!p.success) { res.status(400).json({ error: p.error.errors[0].message }); return; }
  const ledger = await prisma.ledger.create({
    data: { ...p.data, inviteCode: genCode(), owner: { connect: { id: req.userId! } }, members: { create: { user: { connect: { id: req.userId! } } } } },
    include: { members: { include: { user: { select: { id: true, nickname: true } } } } },
  });
  res.status(201).json({ ledger });
});

router.get('/:id(\\d+)', async (req: AuthRequest, res: Response): Promise<void> => {
  const lid = parseInt(req.params.id);
  const m = await prisma.ledgerMember.findUnique({ where: { ledgerId_userId: { ledgerId: lid, userId: req.userId! } } });
  if (!m) { res.status(403).json({ error: '非成员' }); return; }
  const ledger = await prisma.ledger.findUnique({
    where: { id: lid },
    include: { members: { include: { user: { select: { id: true, nickname: true } } } }, expenses: { include: { splits: true }, orderBy: { createdAt: 'desc' } }, settlements: { orderBy: { settledAt: 'desc' } } },
  });
  res.json({ ledger });
});

router.post('/:id(\\d+)/join', async (req: AuthRequest, res: Response): Promise<void> => {
  const lid = parseInt(req.params.id);
  const ledger = await prisma.ledger.findUnique({ where: { id: lid } });
  if (!ledger || ledger.inviteCode !== req.body.inviteCode) { res.status(400).json({ error: '邀请码错误' }); return; }
  const ex = await prisma.ledgerMember.findUnique({ where: { ledgerId_userId: { ledgerId: lid, userId: req.userId! } } });
  if (ex) { res.status(400).json({ error: '已是成员' }); return; }
  await prisma.ledgerMember.create({ data: { ledgerId: lid, userId: req.userId! } });
  res.json({ message: '加入成功' });
});

// DELETE all ledgers owned by current user (clear before re-upload)
router.delete('/my', async (req: AuthRequest, res: Response): Promise<void> => {
  const owned = await prisma.ledger.findMany({ where: { ownerId: req.userId }, select: { id: true } });
  const ids = owned.map(l => l.id);
  // Delete all owned ledgers (cascade deletes members, expenses, settlements)
  await prisma.ledger.deleteMany({ where: { id: { in: ids } } });
  res.json({ deleted: ids.length });
});

router.post('/join-by-code', async (req: AuthRequest, res: Response): Promise<void> => {
  const { inviteCode } = req.body;
  if (!inviteCode) { res.status(400).json({ error: '请输入邀请码' }); return; }
  const ledger = await prisma.ledger.findUnique({ where: { inviteCode } });
  if (!ledger) { res.status(404).json({ error: '邀请码不存在' }); return; }
  const ex = await prisma.ledgerMember.findUnique({ where: { ledgerId_userId: { ledgerId: ledger.id, userId: req.userId! } } });
  if (ex) { res.status(400).json({ error: '已是成员' }); return; }
  await prisma.ledgerMember.create({ data: { ledgerId: ledger.id, userId: req.userId! } });
  res.json({ ledgerId: ledger.id, ledgerName: ledger.name, message: '加入成功' });
});

export { router as ledgersRouter };
