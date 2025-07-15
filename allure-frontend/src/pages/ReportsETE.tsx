// src/pages/ReportsETE.tsx
/* -----------------------------------------------------------------------
 *  src/pages/ReportsETE.tsx
 *  Страница «Отчёты ETE» (полная версия) с унифицированным хедером
 * --------------------------------------------------------------------- */

import {
  AppBar,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
  Tooltip,           // ← вот он!
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef, GridRowSelectionModel } from '@mui/x-data-grid';
import UploadIcon  from '@mui/icons-material/CloudUpload';
import DeleteIcon  from '@mui/icons-material/DeleteForever';
import SearchIcon  from '@mui/icons-material/Search';
import { Logotype, Button as BccButton, EButtonVariant } from 'bcc-design';
import dayjs   from 'dayjs';
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

/* ====================== Типы ====================== */
interface Report {
  uuid:            string;
  createdDateTime: string;
  path:            string;
  active:          boolean;
  size:            number;
  buildUrl:        string;
}

/* маленький помощник для fetch JSON ---------------- */
const fetchJSON = async <T,>(input: RequestInfo, init?: RequestInit) =>
  fetch(input, init).then(r => {
    if (!r.ok) throw new Error(r.statusText);
    return r.json() as Promise<T>;
  });

/* ===================== Компонент =================== */
export default function ReportsETE() {
  const theme    = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  /* ---------- роль пользователя ------------------- */
  const [isAdmin, setIsAdmin] = useState(false);
  useEffect(() => {
    fetchJSON<{ roles: string[] }>('/api/user/info')
      .then(u => setIsAdmin(u.roles?.includes('ROLE_ADMIN') ?? false))
      .catch(() => setIsAdmin(false));
  }, []);

  /* ---------- данные ------------------------------ */
  const [rows,    setRows]   = useState<Report[]>([]);
  const [loading, setLoad]   = useState(false);
  const [sel,     setSel]    = useState<string[]>([]);
  const [filter,  setFilter] = useState('');

  /** Загружаем всё → фильтруем только test (как во Vaadin) */
  const loadRows = useCallback(() => {
    setLoad(true);
    fetchJSON<Report[]>('/api/reports?path=e2e')
      .then(setRows)
      .catch(() => setRows([]))
      .finally(() => setLoad(false));
  }, []);

  useEffect(() => { loadRows(); }, [loadRows]);

  /* ---------- колонки ----------------------------- */
  const columns: GridColDef[] = useMemo(() => {
    const base: GridColDef[] = [
      { field: 'createdDateTime', headerName: 'Created', width: 140,
        valueFormatter: ({ value }) => dayjs.utc(value as string)   // ← трактуем строку как UTC
                                                              .local()                // ← переводим в местную зону
                                                              .format('DD-MM-YYYY HH:mm'),
      },
      {
            field: 'url',
            headerName: 'Url',
            flex: 1,
            minWidth: 200,
            sortable: false,
            // мы не используем valueGetter, а сразу рендерим всё в renderCell
            renderCell: (params) => {
              const { path, uuid } = params.row as Report;
              const display = `reports/${path}`;                               // что отображаем
              const href = `/allure/reports/${uuid}/index.html`;               // куда ведём
              return (
                <a href={href} target="_blank" rel="noopener noreferrer">
                  {display}
                </a>
              );
            },
          },
    ];

    if (isAdmin) {
      return [
        { field: 'uuid', headerName: 'ID', minWidth: 300 },
        ...base,
        { field: 'path', headerName: 'Path', flex: 1, minWidth: 120 },
        { field: 'active', headerName: 'Active', width: 90 },
        {
          field: 'size', headerName: 'Size KB', width: 100, type: 'number',
          valueGetter: ({ row }) => row.size,
        },
      ];
    }
    return base;
  }, [isAdmin]);

  /* ---------- фильтр поиска ----------------------- */
  const visibleRows = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter(r =>
      Object.values(r).join(' ').toLowerCase().includes(q),
    );
  }, [rows, filter]);

  /* ---------- диалог загрузки --------------------- */
  const [upOpen, setUpOpen] = useState(false);
    const fileRef = useRef<HTMLInputElement | null>(null);

    const handleUpload = async () => {
      const file = fileRef.current?.files?.[0];
      if (!file) return;

      const form = new FormData();
      form.append('allureReportArchive', file);           // имя part == @RequestParam

      // «reportPath» = имя архива без .zip
      const reportPath = encodeURIComponent(
        file.name.replace(/\.zip$/i, ''));

      try {
        const resp = await fetch(`/api/report/${reportPath}`, {
          method:        'POST',
          body:          form,
          credentials:   'same-origin',
        });
        if (!resp.ok) throw new Error(`${resp.status} ${resp.statusText}`);
        loadRows();
        setUpOpen(false);
      } catch (e) {
        alert('Ошибка загрузки: ' + (e as Error).message);
      }
    };

  /* ---------- удаление ---------------------------- */
  const handleDelete = async () => {
    if (!sel.length) return;
    try {
      await Promise.all(
        sel.map(uuid =>
          fetch('/api/reports/' + uuid, { method: 'DELETE' })
            .then(res => { if (!res.ok) throw new Error(res.statusText); })
        )
      );
      setSel([]);
      loadRows();
    } catch (e) {
      alert('Ошибка при удалении: ' + (e as Error).message);
    }
  };

  /* ===================== UI ======================= */
  return (
    <>
      {/* ─────────── Header ─────────── */}
      <AppBar
        position="fixed"
        sx={{
          zIndex: (t) => t.zIndex.drawer + 1,
          backgroundColor: '#2dba8d',
          color: '#000',
        }}
      >
        <Toolbar sx={{ minHeight: 56, px: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography
              component="span"
              sx={{
                fontFamily: '"Inter", sans-serif',
                fontSize: '1.25rem',
                fontWeight: 600,
                lineHeight: 1,
                transform: 'translateY(-2px)',
              }}
            >
              Reports
            </Typography>
            <Box sx={{ display: 'inline-block', transform: 'translateY(2px)' }}>
              <Logotype.BccHub size="sm" />
            </Box>
          </Box>
          <Box sx={{ flexGrow: 1 }} />
          <Box
            sx={{
              px: isMobile ? 2 : 3,
              py: isMobile ? 0.75 : 1,
              backgroundColor: '#E6E7EB',
              color: '#2E3034',
              borderRadius: 2,
              fontWeight: 600,
              display: 'inline-block',
              '&:hover': { backgroundColor: '#DADBDF' },
            }}
          >
            <BccButton
              view={EButtonVariant.NeutralFilledSecondary}
              size={isMobile ? 'm' : 'l'}
              onClick={() => (window.location.href = '/ui/reports-main')}
              style={{
                background: 'transparent',
                border: 0,
                padding: 0,
                minWidth: 0,
                color: 'inherit',
                font: 'inherit',
              }}
            >
              Вернуться&nbsp;на&nbsp;главную
            </BccButton>
          </Box>
        </Toolbar>
      </AppBar>

      {/* ─────────── Main Content ─────────── */}
      <Box component="main" sx={{ flexGrow: 1, p: 4, mt: '56px' }}>
        {/* --- Toolbar поиска/кнопок --- */}
        <Stack
          direction={isMobile ? 'column' : 'row'}
          spacing={2}
          alignItems="center"
          mb={2}
        >
          <TextField
            size="small"
            placeholder="Поиск…"
            value={filter}
            onChange={e => setFilter(e.target.value)}
            InputProps={{ startAdornment: <SearchIcon fontSize="small" /> }}
          />

          <Tooltip title="Загрузить отчёт">
            <span>
              <Button
                variant="contained"
                startIcon={<UploadIcon />}
                onClick={() => setUpOpen(true)}
              >
                Upload
              </Button>
            </span>
          </Tooltip>

          {isAdmin && (
            <Tooltip title="Удалить выбранное">
              <span>
                <Button
                  variant="outlined"
                  color="error"
                  startIcon={<DeleteIcon />}
                  disabled={!sel.length}
                  onClick={handleDelete}
                >
                  Delete
                </Button>
              </span>
            </Tooltip>
          )}
        </Stack>

        {/* --- Таблица --- */}
        <DataGrid
          //autoHeight
          rows={visibleRows}
          columns={columns}
          getRowId={(r: Report) => r.uuid}
          checkboxSelection={isAdmin}
          disableRowSelectionOnClick
          loading={loading}
          onRowSelectionModelChange={(m: GridRowSelectionModel) =>
            setSel(m as string[])
          }
          sx={{ bgcolor: 'grey.100', height: 700, }}
        />

        {/* --- Диалог загрузки --- */}
        <Dialog open={upOpen} onClose={() => setUpOpen(false)} fullWidth>
          <DialogTitle>Загрузка отчёта (.zip)</DialogTitle>
          <DialogContent>
            <input
              type="file"
              accept=".zip"
              ref={ref => (fileRef.current = ref)}
              style={{ marginTop: 16 }}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setUpOpen(false)}>Отмена</Button>
            <Button variant="contained" onClick={handleUpload}>
              Загрузить
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </>
  );
}
