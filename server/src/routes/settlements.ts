import { Router, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { z } from 'zod';
import { authMiddleware, AuthRequest } from '../middleware/auth';

const router = Router();
const prisma = new PrismaClient();
router.use(authMiddleware);

router.get('/:lid(\\d+)', async (req: AuthRequest, res: Response): Promise<void> => {
  const lid = parseInt(req.params.lid);
  const settlements = await prisma.settlement.findMany({ where: { ledgerId: lid }, orderBy: { settledAt: 'desc' } });
  res.json({ settlements });
});

router.post('/:lid(\\d+)', async (req: AuthRequest, res: Response): Promise<void> => {
  const lid = parseInt(req.params.lid);
  const p = z.array(z.object({ fromMemberId: z.number().int(), toMemberId: z.number().int(), amountCny: z.number().positive(), isPaid: z.boolean().optional() })).safeParse(req.body);
  if (!p.success) { res.status(400).json({ error: p.error.errors[0].message }); return; }
  await prisma.settlement.deleteMany({ where: { ledgerId: lid } });
  const created = await Promise.all(p.data.map(s => prisma.settlement.create({ data: { ledgerId: lid, fromMemberId: s.fromMemberId, toMemberId: s.toMemberId, amountCny: s.amountCny, isPaid: s.isPaid ?? false } })));
  await prisma.ledger.update({ where: { id: lid }, data: { updatedAt: new Date() } });
  res.status(201).json({ settlements: created });
});

router.put('/:lid(\\d+)/:id(\\d+)/pay', async (req: AuthRequest, res: Response): Promise<void> => {
  const settlement = await prisma.settlement.update({ where: { id: parseInt(req.params.id) }, data: { isPaid: true, paidAt: new Date() } });
  res.json({ settlement });
});

export { router as settlementsRouter };
