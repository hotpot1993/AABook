import { Router, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { authMiddleware, AuthRequest } from '../middleware/auth';

const router = Router();
const prisma = new PrismaClient();
router.use(authMiddleware);

// Check if current user is admin
async function isAdmin(userId: number): Promise<boolean> {
  const user = await prisma.user.findUnique({ where: { id: userId } });
  return user?.nickname === 'admin';
}

// GET /api/admin/users — list all users
router.get('/users', async (req: AuthRequest, res: Response): Promise<void> => {
  if (!(await isAdmin(req.userId!))) { res.status(403).json({ error: '非管理员' }); return; }
  const users = await prisma.user.findMany({
    select: { id: true, nickname: true, createdAt: true },
    orderBy: { id: 'asc' },
  });
  res.json({ users });
});

// DELETE /api/admin/users/:id — delete user
router.delete('/users/:id(\\d+)', async (req: AuthRequest, res: Response): Promise<void> => {
  if (!(await isAdmin(req.userId!))) { res.status(403).json({ error: '非管理员' }); return; }
  const id = parseInt(req.params.id);
  if (id === req.userId) { res.status(400).json({ error: '不能删除自己' }); return; }
  await prisma.user.delete({ where: { id } });
  res.json({ message: '已删除' });
});

// GET /api/admin/check — check if current user is admin
router.get('/check', async (req: AuthRequest, res: Response): Promise<void> => {
  res.json({ isAdmin: await isAdmin(req.userId!) });
});

export { router as adminRouter };
