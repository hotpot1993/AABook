import { Router, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { z } from 'zod';
import { authMiddleware, AuthRequest } from '../middleware/auth';

const router = Router();
const prisma = new PrismaClient();
router.use(authMiddleware);

const schema = z.object({
  title: z.string().min(1), totalAmountCny: z.number().positive(),
  originalCurrency: z.string().optional(), originalAmount: z.number().optional(),
  exchangeRate: z.number().optional(), category: z.string().optional(),
  paidByMemberId: z.number().int(), note: z.string().optional(), receiptUri: z.string().nullable().optional(),
  splits: z.array(z.object({ memberId: z.number().int(), shareType: z.string(), shareValue: z.number(), amountCny: z.number() })),
});

async function checkMember(lid: number, uid: number): Promise<boolean> {
  return !!(await prisma.ledgerMember.findUnique({ where: { ledgerId_userId: { ledgerId: lid, userId: uid } } }));
}

router.post('/:lid(\\d+)', async (req: AuthRequest, res: Response): Promise<void> => {
  const lid = parseInt(req.params.lid);
  if (!(await checkMember(lid, req.userId!))) { res.status(403).json({ error: '非成员' }); return; }
  const p = schema.safeParse(req.body);
  if (!p.success) { res.status(400).json({ error: p.error.errors[0].message }); return; }
  const { splits, ...data } = p.data;
  const expense = await prisma.expense.create({ data: { ...data, ledgerId: lid, splits: { create: splits } }, include: { splits: true } });
  await prisma.ledger.update({ where: { id: lid }, data: { updatedAt: new Date() } });
  res.status(201).json({ expense });
});

router.put('/:lid(\\d+)/:eid(\\d+)', async (req: AuthRequest, res: Response): Promise<void> => {
  const lid = parseInt(req.params.lid), eid = parseInt(req.params.eid);
  if (!(await checkMember(lid, req.userId!))) { res.status(403).json({ error: '非成员' }); return; }
  const p = schema.safeParse(req.body);
  if (!p.success) { res.status(400).json({ error: p.error.errors[0].message }); return; }
  const { splits, ...data } = p.data;
  await prisma.expenseSplit.deleteMany({ where: { expenseId: eid } });
  const updated = await prisma.expense.update({ where: { id: eid }, data: { ...data, splits: { create: splits } }, include: { splits: true } });
  res.json({ expense: updated });
});

router.delete('/:lid(\\d+)/:eid(\\d+)', async (req: AuthRequest, res: Response): Promise<void> => {
  const lid = parseInt(req.params.lid), eid = parseInt(req.params.eid);
  if (!(await checkMember(lid, req.userId!))) { res.status(403).json({ error: '非成员' }); return; }
  await prisma.expense.delete({ where: { id: eid } });
  res.json({ message: '删除成功' });
});

export { router as expensesRouter };
