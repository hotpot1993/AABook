import { Router, Request, Response } from 'express';
import bcrypt from 'bcryptjs';
import { PrismaClient } from '@prisma/client';
import { z } from 'zod';
import { generateToken } from '../middleware/auth';

const router = Router();
const prisma = new PrismaClient();

const schema = z.object({
  nickname: z.string().min(2).max(20),
  password: z.string().min(6).max(100),
});

router.post('/login', async (req: Request, res: Response): Promise<void> => {
  try {
    const p = schema.safeParse(req.body);
    if (!p.success) { res.status(400).json({ error: p.error.errors[0].message }); return; }
    const { nickname, password } = p.data;

    let user = await prisma.user.findUnique({ where: { nickname } });
    if (user) {
      const valid = await bcrypt.compare(password, user.password);
      if (!valid) { res.status(401).json({ error: '密码错误' }); return; }
    } else {
      user = await prisma.user.create({ data: { nickname, password: await bcrypt.hash(password, 10) } });
    }
    res.json({ token: generateToken(user.id, user.nickname), user: { id: user.id, nickname: user.nickname } });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: '服务器错误' });
  }
});

export { router as authRouter };
